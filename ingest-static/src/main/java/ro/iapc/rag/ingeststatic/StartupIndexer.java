package ro.iapc.rag.ingeststatic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ro.iapc.rag.store.VectorIndex;
import ro.iapc.rag.store.VectorStoreProvider;

/**
 * Creates the default vector index on first start (other services wait for ingest-static to be healthy), then
 * optionally ingests ./resources.
 */
@Component
class StartupIndexer {

    private static final Logger log = LoggerFactory.getLogger(StartupIndexer.class);

    private final VectorStoreProvider stores;
    private final PdfIngestionService ingestion;
    private final IngestProperties properties;

    StartupIndexer(VectorStoreProvider stores, PdfIngestionService ingestion, IngestProperties properties) {
        this.stores = stores;
        this.ingestion = ingestion;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        VectorIndex active = stores.ensureActiveIndex();
        log.info("Active vector index: {} ({})", active.tableName(), active.embeddingModel());
        if (properties.syncOnStartup()) {
            ingestion.syncAll();
        }
    }
}
