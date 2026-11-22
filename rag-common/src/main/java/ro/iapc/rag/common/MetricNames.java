package ro.iapc.rag.common;

/**
 * Custom Micrometer metric names. The "RAG pipeline" Grafana dashboard queries these, so renaming one breaks a panel.
 * Prometheus sees dots as underscores and timers get a {@code _seconds} suffix.
 */
public final class MetricNames {

    /** Timer, tag {@code stage}: pre, post, expand, rerank, stitch, llm, embed. */
    public static final String STAGE_DURATION = "rag.stage.duration";

    /** Counter of answers that fell back to "I don't know". */
    public static final String REFUSALS = "rag.refusals";

    /** Counter, tag {@code type}: prompt or completion. */
    public static final String LLM_TOKENS = "rag.llm.tokens";

    /** Counter of chunks written, tag {@code source_type}: static or dynamic. */
    public static final String INGESTED_CHUNKS = "rag.ingest.chunks";

    /** Counter of failed ingestion jobs, tag {@code source_type}. */
    public static final String INGEST_FAILURES = "rag.ingest.failures";

    public static final String TAG_STAGE = "stage";

    private MetricNames() {}
}
