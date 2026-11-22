package ro.iapc.rag.ingeststatic;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ManifestRepository {

    private static final RowMapper<ManifestEntry> ROW = (rs, i) -> new ManifestEntry(
            rs.getString("doc_id"),
            rs.getString("version"),
            rs.getString("source"),
            rs.getString("checksum"),
            rs.getString("embedding_model"),
            rs.getString("table_name"),
            rs.getInt("chunk_count"),
            rs.getObject("ingested_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public ManifestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(ManifestEntry e) {
        jdbc.update(
                """
                insert into ingestion_manifest
                    (doc_id, version, source, checksum, embedding_model, table_name, chunk_count, ingested_at)
                values (?, ?, ?, ?, ?, ?, ?, now())
                on conflict (doc_id, version, table_name) do update set
                    source = excluded.source, checksum = excluded.checksum, embedding_model = excluded.embedding_model,
                    chunk_count = excluded.chunk_count, ingested_at = now()
                """,
                e.docId(),
                e.version(),
                e.source(),
                e.checksum(),
                e.embeddingModel(),
                e.tableName(),
                e.chunkCount());
    }

    public List<ManifestEntry> findAll() {
        return jdbc.query("select * from ingestion_manifest order by doc_id, version", ROW);
    }

    public int deleteByDocId(String docId, String tableName) {
        return jdbc.update("delete from ingestion_manifest where doc_id = ? and table_name = ?", docId, tableName);
    }
}
