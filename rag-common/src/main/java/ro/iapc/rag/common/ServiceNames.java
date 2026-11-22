package ro.iapc.rag.common;

/** Eureka service ids. They match {@code spring.application.name} of each module and the gateway route prefixes. */
public final class ServiceNames {

    public static final String RAG_API = "rag-api";
    public static final String INGEST_STATIC = "ingest-static";
    public static final String INGEST_DYNAMIC = "ingest-dynamic";
    public static final String RETRIEVAL_PRE = "retrieval-pre";
    public static final String RETRIEVAL_POST = "retrieval-post";

    private ServiceNames() {}
}
