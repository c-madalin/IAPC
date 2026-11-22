package ro.iapc.rag.ingeststatic;

import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.MetricNames;
import ro.iapc.rag.store.ChunkQueries;
import ro.iapc.rag.store.VectorIndex;
import ro.iapc.rag.store.VectorStoreProvider;

/**
 * Walking-skeleton ingestion: page-based PDF reading, token splitting, delete-then-add per document. Story 1 replaces
 * this with structure-aware chunking with overlap (Task 1), checksum skip and atomic replacement (Task 2), orphan
 * checks (Task 3) and re-index (Task 4).
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);

    private final VectorStoreProvider stores;
    private final ChunkQueries chunkQueries;
    private final ManifestRepository manifest;
    private final IngestProperties properties;
    private final MeterRegistry meters;

    public PdfIngestionService(
            VectorStoreProvider stores,
            ChunkQueries chunkQueries,
            ManifestRepository manifest,
            IngestProperties properties,
            MeterRegistry meters) {
        this.stores = stores;
        this.chunkQueries = chunkQueries;
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
        VectorStore store = stores.forIndex(index);
        String filename = pdf.getFilename();
        String docId = docIdFor(filename);
        String checksum = sha256(pdf);

        List<Document> pages = new PagePdfDocumentReader(pdf).read();
        List<Document> chunks = TokenTextSplitter.builder()
                .withChunkSize(properties.chunkSize())
                .build()
                .apply(pages);
        for (int i = 0; i < chunks.size(); i++) {
            var metadata = chunks.get(i).getMetadata();
            metadata.put(MetadataKeys.DOC_ID, docId);
            metadata.put(MetadataKeys.VERSION, "1");
            metadata.put(MetadataKeys.SOURCE, filename);
            metadata.put(MetadataKeys.PAGE, metadata.getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, 0));
            metadata.put(MetadataKeys.CHUNK_INDEX, i);
            metadata.put(MetadataKeys.CHECKSUM, checksum);
            metadata.put(MetadataKeys.IS_LATEST, true);
            metadata.put(MetadataKeys.EMBEDDING_MODEL, index.embeddingModel());
        }

        // TODO(story-1-task-2): not atomic. A query between delete and add sees no chunks for this document.
        store.delete(
                new FilterExpressionBuilder().eq(MetadataKeys.DOC_ID, docId).build());
        store.add(chunks);

        ManifestEntry entry = new ManifestEntry(
                docId, "1", filename, checksum, index.embeddingModel(), index.tableName(), chunks.size(), null);
        manifest.upsert(entry);
        meters.counter(MetricNames.INGESTED_CHUNKS, "source_type", "static").increment(chunks.size());
        log.info("Ingested {} as {} ({} chunks into {})", filename, docId, chunks.size(), index.tableName());
        return entry;
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

    static String sha256(Resource resource) {
        try (InputStream in = resource.getInputStream();
                DigestInputStream digest = new DigestInputStream(in, MessageDigest.getInstance("SHA-256"))) {
            digest.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digest.getMessageDigest().digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot hash " + resource.getFilename(), e);
        }
    }
}
