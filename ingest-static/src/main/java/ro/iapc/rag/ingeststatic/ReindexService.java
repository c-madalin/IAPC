package ro.iapc.rag.ingeststatic;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.store.ChunkQueries;
import ro.iapc.rag.store.IndexRegistry;
import ro.iapc.rag.store.ManifestRepository;
import ro.iapc.rag.store.VectorIndex;
import ro.iapc.rag.store.VectorStoreProvider;

@Service
public class ReindexService {

    private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

    private final VectorStoreProvider stores;
    private final ChunkQueries chunks;
    private final ManifestRepository manifest;
    private final IndexRegistry registry;

    public ReindexService(
            VectorStoreProvider stores, ChunkQueries chunks, ManifestRepository manifest, IndexRegistry registry) {
        this.stores = stores;
        this.chunks = chunks;
        this.manifest = manifest;
        this.registry = registry;
    }

    public record Result(String fromTable, String toTable, String model, int chunks, int drift) {}

    public Result reindex(String model) {
        VectorIndex source = stores.activeIndex();
        VectorIndex target = stores.createIndex(model);
        if (source.tableName().equals(target.tableName())) {
            throw new IllegalArgumentException("Re-index model must differ from the active model");
        }
        List<Document> latest = chunks.findLatest(source);
        for (Document chunk : latest) {
            chunk.getMetadata().put(MetadataKeys.EMBEDDING_MODEL, model);
        }
        if (!latest.isEmpty()) {
            stores.forIndex(target).add(latest);
        }

        manifest.copyForIndex(source, target);
        ManifestRepository.Reconciliation report = manifest.reconcile(target, chunks);
        if (report.drift() != 0) {
            throw new IllegalStateException("Re-index drift detected: manifest=" + report.manifestChunks() + ", actual="
                    + report.actualChunks());
        }

        registry.activate(target.tableName());
        log.info(
                "Re-indexed {} chunks from {} to {} with {}",
                latest.size(),
                source.tableName(),
                target.tableName(),
                model);
        return new Result(source.tableName(), target.tableName(), model, latest.size(), report.drift());
    }
}
