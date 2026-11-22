package ro.iapc.rag.common;

/**
 * Metadata keys every chunk in the vector store carries. Ingestion writes them, retrieval filters and cites by them.
 * Add a key here before using it anywhere else, so the services agree on the spelling.
 */
public final class MetadataKeys {

    /** Stable id of the source document. Same file or URL, same id, across versions. */
    public static final String DOC_ID = "doc_id";

    public static final String VERSION = "version";
    public static final String SOURCE = "source";
    public static final String PAGE = "page";
    public static final String CHUNK_INDEX = "chunk_index";

    /** SHA-256 of the source file or fetched body. */
    public static final String CHECKSUM = "checksum";

    public static final String IS_LATEST = "is_latest";
    public static final String VALID_FROM = "valid_from";
    public static final String SOURCE_URL = "source_url";
    public static final String FETCHED_AT = "fetched_at";
    public static final String EMBEDDING_MODEL = "embedding_model";

    private MetadataKeys() {}
}
