package ro.iapc.rag.ingeststatic;

import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.MetricNames;
import ro.iapc.rag.ingest.Checksums;
import ro.iapc.rag.ingest.ChunkMetadata;
import ro.iapc.rag.ingest.PdfChunker;
import ro.iapc.rag.store.ChunkQueries;
import ro.iapc.rag.store.ChunkWriter;
import ro.iapc.rag.store.ManifestEntry;
import ro.iapc.rag.store.ManifestRepository;
import ro.iapc.rag.store.VectorIndex;
import ro.iapc.rag.store.VectorStoreProvider;

/**
 * Ingests rag-ingest chunks, skips unchanged static files and replaces changed documents atomically.
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);

    private final VectorStoreProvider stores;
    private final ChunkQueries chunkQueries;
    private final ChunkWriter chunkWriter;
    private final ManifestRepository manifest;
    private final IngestProperties properties;
    private final MeterRegistry meters;

    public PdfIngestionService(
            VectorStoreProvider stores,
            ChunkQueries chunkQueries,
            ChunkWriter chunkWriter,
            ManifestRepository manifest,
            IngestProperties properties,
            MeterRegistry meters) {
        this.stores = stores;
        this.chunkQueries = chunkQueries;
        this.chunkWriter = chunkWriter;
        this.manifest = manifest;
        this.properties = properties;
        this.meters = meters;
    }

    /** Ingests every PDF matching {@code rag.ingest.resources}. A failing file is logged and skipped. */
    public List<ManifestEntry> syncAll() {
        Resource[] pdfs;
        try {
            pdfs = new PathMatchingResourcePatternResolver().getResources(properties.resources());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + properties.resources(), e);
        }
        log.info("Syncing {} PDFs from {}", pdfs.length, properties.resources());
        List<ManifestEntry> results = new ArrayList<>();
        for (Resource pdf : pdfs) {
            try {
                results.add(ingest(pdf));
            } catch (RuntimeException e) {
                log.error("Failed to ingest {}", pdf.getFilename(), e);
                meters.counter(MetricNames.INGEST_FAILURES, "source_type", "static")
                        .increment();
            }
        }
        return results;
    }

    public ManifestEntry ingest(Resource pdf) {
        VectorIndex index = stores.activeIndex();
        String filename = pdf.getFilename();
        String docId = docIdFor(filename);
        String checksum = Checksums.sha256(pdf);
        Optional<ManifestEntry> existing = manifest.findLatest(docId, index.tableName());
        if (existing.isPresent() && checksum.equals(existing.get().checksum())) {
            log.info("Skipped unchanged {} ({})", filename, docId);
            return existing.get();
        }
        String version = nextVersion(existing);

        List<Document> chunks = new ChunkMetadata(docId, version, filename, checksum, index.embeddingModel())
                .applyTo(new PdfChunker(properties.chunkSize(), properties.chunkOverlap()).chunk(pdf));
        ChunkWriter.Result written = chunkWriter.write(index, docId, chunks, ChunkWriter.Retire.DELETE);

        ManifestEntry entry = new ManifestEntry(
                docId,
                version,
                filename,
                ManifestEntry.SourceType.STATIC,
                checksum,
                index.embeddingModel(),
                index.tableName(),
                chunks.size(),
                null);
        manifest.upsert(entry);
        meters.counter(MetricNames.INGESTED_CHUNKS, "source_type", "static").increment(chunks.size());
        log.info(
                "Ingested {} as {} ({} chunks into {}, {} old chunks deleted)",
                filename,
                docId,
                written.published(),
                index.tableName(),
                written.retired());
        return entry;
    }

    static String nextVersion(Optional<ManifestEntry> existing) {
        if (existing.isEmpty()) {
            return "1";
        }
        try {
            return Integer.toString(Integer.parseInt(existing.get().version()) + 1);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Static document version must be an integer, got "
                            + existing.get().version(),
                    e);
        }
    }

    /** Returns the number of chunks removed; 0 means the document was not in the index. */
    public int delete(String docId) {
        VectorIndex index = stores.activeIndex();
        int count = chunkQueries.countByDocId(index, docId);
        if (count > 0) {
            stores.forIndex(index)
                    .delete(new FilterExpressionBuilder()
                            .eq(MetadataKeys.DOC_ID, docId)
                            .build());
        }
        manifest.deleteByDocId(docId, index.tableName());
        log.info("Deleted {} chunks of {}", count, docId);
        return count;
    }

    static String docIdFor(String filename) {
        return "static:" + filename;
    }
}
