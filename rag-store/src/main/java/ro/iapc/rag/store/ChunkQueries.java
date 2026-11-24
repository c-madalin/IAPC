package ro.iapc.rag.store;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.api.Chunk;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lookups by metadata that a similarity search can't express. Table names come from {@link IndexRegistry}, never
 * from user input.
 */
public class ChunkQueries {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<java.util.Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;

    public ChunkQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Published chunks of one document version with {@code chunk_index} in {@code [from, to]}, ordered by index. With
     * a null {@code version}, every published version matches. Staged chunks (see {@link ChunkWriter}) never do.
     */
    public List<Chunk> byIndexRange(VectorIndex index, String docId, String version, int from, int to) {
        String sql = "select id, content, metadata from " + index.tableName()
                + " where metadata->>'" + MetadataKeys.DOC_ID + "' = ?"
                + " and metadata->>'" + MetadataKeys.STAGED_VERSION + "' is null"
                + " and (cast(? as text) is null or metadata->>'" + MetadataKeys.VERSION + "' = ?)"
                + " and (metadata->>'" + MetadataKeys.CHUNK_INDEX + "')::int between ? and ?"
                + " order by (metadata->>'" + MetadataKeys.CHUNK_INDEX + "')::int";
        return jdbc.query(sql, rowMapper(), docId, version, version, from, to);
    }

    /** Every chunk of the document, staged ones included, so a delete reports what it removes. */
    public int countByDocId(VectorIndex index, String docId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from " + index.tableName() + " where metadata->>'" + MetadataKeys.DOC_ID + "' = ?",
                Integer.class,
                docId);
        return count == null ? 0 : count;
    }

    /** Published latest chunks in reading order. Staged chunks are never copied during re-indexing. */
    public List<Document> findLatest(VectorIndex index) {
        String sql = "select id, content, metadata from " + index.tableName()
                + " where metadata->>'" + MetadataKeys.IS_LATEST + "' = 'true'"
                + " and metadata->>'" + MetadataKeys.STAGED_VERSION + "' is null"
                + " order by metadata->>'" + MetadataKeys.DOC_ID + "',"
                + " (metadata->>'" + MetadataKeys.CHUNK_INDEX + "')::int";
        return jdbc.query(
                sql,
                (rs, i) -> Document.builder()
                        .text(rs.getString("content"))
                        .metadata(JSON.readValue(rs.getString("metadata"), MAP))
                        .build());
    }

    private static RowMapper<Chunk> rowMapper() {
        return (rs, i) -> ChunkMapper.toChunk(Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .metadata(JSON.readValue(rs.getString("metadata"), MAP))
                .build());
    }
}
