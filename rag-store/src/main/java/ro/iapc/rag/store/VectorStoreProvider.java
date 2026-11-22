package ro.iapc.rag.store;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIdType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hands out a Spring AI {@link VectorStore} per vector table. Tables are named {@code vector_store_<model>} and created
 * by Spring AI's own DDL, so the layout always matches what PgVectorStore reads and writes.
 */
public class VectorStoreProvider {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreProvider.class);
    private static final Pattern NON_IDENT = Pattern.compile("[^a-z0-9]+");

    private final JdbcTemplate jdbc;
    private final IndexRegistry registry;
    private final EmbeddingModelFactory embeddingModels;
    private final RagStoreProperties properties;
    private final Map<String, VectorStore> stores = new ConcurrentHashMap<>();

    public VectorStoreProvider(
            JdbcTemplate jdbc,
            IndexRegistry registry,
            EmbeddingModelFactory embeddingModels,
            RagStoreProperties properties) {
        this.jdbc = jdbc;
        this.registry = registry;
        this.embeddingModels = embeddingModels;
        this.properties = properties;
    }

    public static String tableNameFor(String model) {
        String ident = NON_IDENT.matcher(model.toLowerCase(Locale.ROOT)).replaceAll("_");
        return "vector_store_" + ident.replaceAll("^_|_$", "");
    }

    /** The active index, or an exception if ingest-static has not created one yet. */
    public VectorIndex activeIndex() {
        return registry.active()
                .orElseThrow(() -> new IllegalStateException(
                        "No active vector index. Start ingest-static, which creates one on startup."));
    }

    public VectorStore active() {
        return forIndex(activeIndex());
    }

    public VectorStore forIndex(VectorIndex index) {
        return stores.computeIfAbsent(index.tableName(), table -> build(index, false));
    }

    /**
     * Creates the table for {@code model} if it is missing and registers it as inactive. Call {@link
     * IndexRegistry#activate(String)} once it holds data.
     */
    public VectorIndex createIndex(String model) {
        int dims = properties.dimensionsOf(model);
        String table = tableNameFor(model);
        VectorIndex index = new VectorIndex(table, model, dims, false);
        VectorStore store = build(index, true);
        jdbc.execute("create index if not exists " + table + "_doc_id_idx on " + table + " ((metadata->>'doc_id'))");
        registry.register(table, model, dims);
        stores.put(table, store);
        log.info("Vector index {} ready (model {}, {} dims)", table, model, dims);
        return registry.find(table).orElseThrow();
    }

    /** Creates and activates the default model's index if no index is active. Returns the active index. */
    public VectorIndex ensureActiveIndex() {
        return registry.active().orElseGet(() -> {
            VectorIndex created = createIndex(properties.defaultModel());
            registry.activate(created.tableName());
            log.info("No active index found, activated {}", created.tableName());
            return registry.active().orElseThrow();
        });
    }

    private VectorStore build(VectorIndex index, boolean initializeSchema) {
        PgVectorStore store = PgVectorStore.builder(jdbc, embeddingModels.forModel(index.embeddingModel()))
                .vectorTableName(index.tableName())
                .dimensions(index.dimensions())
                .idType(PgIdType.UUID)
                .distanceType(PgDistanceType.COSINE_DISTANCE)
                .indexType(PgIndexType.HNSW)
                .initializeSchema(initializeSchema)
                .vectorTableValidationsEnabled(!initializeSchema)
                .build();
        store.afterPropertiesSet();
        return store;
    }
}
