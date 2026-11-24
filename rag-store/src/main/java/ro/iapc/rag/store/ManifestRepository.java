package ro.iapc.rag.store;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Reads and writes {@code ingestion_manifest}. Both ingestion services write through it, so reconciliation (Story 1
 * Task 4) sees static and dynamic documents in one table.
 */
public class ManifestRepository {

    private static final RowMapper<ManifestEntry> ROW = (rs, i) -> new ManifestEntry(
            rs.getString("doc_id"),
            rs.getString("version"),
            rs.getString("source"),
            ManifestEntry.SourceType.fromColumn(rs.getString("source_type")),
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
                    (doc_id, version, source, source_type, checksum, embedding_model, table_name, chunk_count,
                     ingested_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, now())
                on conflict (doc_id, version, table_name) do update set
                    source = excluded.source, source_type = excluded.source_type, checksum = excluded.checksum,
                    embedding_model = excluded.embedding_model, chunk_count = excluded.chunk_count,
                    ingested_at = now()
                """,
                e.docId(),
                e.version(),
                e.source(),
                e.sourceType().column(),
                e.checksum(),
                e.embeddingModel(),
                e.tableName(),
                e.chunkCount());
    }

    public List<ManifestEntry> findAll() {
        return jdbc.query("select * from ingestion_manifest order by doc_id, version", ROW);
    }

    public Optional<ManifestEntry> findLatest(String docId, String tableName) {
        List<ManifestEntry> rows = jdbc.query(
                "select * from ingestion_manifest where doc_id = ? and table_name = ?"
                        + " order by ingested_at desc limit 1",
                ROW,
                docId,
                tableName);
        return rows.stream().findFirst();
    }

    public int deleteByDocId(String docId, String tableName) {
        return jdbc.update("delete from ingestion_manifest where doc_id = ? and table_name = ?", docId, tableName);
    }

    public record Reconciliation(
            VectorIndex index, int manifestEntries, int manifestChunks, int actualChunks, int drift) {}

    public Reconciliation reconcile(VectorIndex index, ChunkQueries chunks) {
        List<ManifestEntry> rows =
                jdbc.query("select * from ingestion_manifest where table_name = ?", ROW, index.tableName());
        int manifestChunks = rows.stream().mapToInt(ManifestEntry::chunkCount).sum();
        int actualChunks = jdbc.queryForObject("select count(*) from " + index.tableName(), Integer.class);
        int drift = manifestChunks == actualChunks ? 0 : Math.abs(manifestChunks - actualChunks);
        return new Reconciliation(index, rows.size(), manifestChunks, actualChunks, drift);
    }

    public void copyForIndex(VectorIndex source, VectorIndex target) {
        for (ManifestEntry entry : findAll()) {
            if (source.tableName().equals(entry.tableName())) {
                upsert(new ManifestEntry(
                        entry.docId(),
                        entry.version(),
                        entry.source(),
                        entry.sourceType(),
                        entry.checksum(),
                        target.embeddingModel(),
                        target.tableName(),
                        entry.chunkCount(),
                        null));
            }
        }
    }
}
