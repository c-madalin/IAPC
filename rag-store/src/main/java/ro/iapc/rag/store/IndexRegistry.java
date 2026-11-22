package ro.iapc.rag.store;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads and writes the {@code index_registry} table (Flyway V1). Services cache the active index for
 * {@link RagStoreProperties#registryCacheTtl()}, so a switch after a re-index reaches every service within that time.
 */
public class IndexRegistry {

    private static final RowMapper<VectorIndex> ROW = (rs, i) -> new VectorIndex(
            rs.getString("table_name"),
            rs.getString("embedding_model"),
            rs.getInt("dimensions"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final RagStoreProperties properties;
    private final Clock clock;

    private volatile Optional<VectorIndex> cachedActive;
    private volatile Instant cachedAt = Instant.MIN;

    public IndexRegistry(JdbcTemplate jdbc, TransactionTemplate tx, RagStoreProperties properties, Clock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<VectorIndex> active() {
        Instant now = clock.instant();
        if (cachedActive == null || now.isAfter(cachedAt.plus(properties.registryCacheTtl()))) {
            cachedActive = jdbc.query("select * from index_registry where active", ROW).stream()
                    .findFirst();
            cachedAt = now;
        }
        return cachedActive;
    }

    public List<VectorIndex> all() {
        return jdbc.query("select * from index_registry order by created_at", ROW);
    }

    public Optional<VectorIndex> find(String tableName) {
        return jdbc.query("select * from index_registry where table_name = ?", ROW, tableName).stream()
                .findFirst();
    }

    void register(String tableName, String model, int dimensions) {
        jdbc.update(
                "insert into index_registry (table_name, embedding_model, dimensions, active) values (?, ?, ?, false)"
                        + " on conflict (table_name) do nothing",
                tableName,
                model,
                dimensions);
    }

    /** Makes {@code tableName} the only active index, in one transaction. */
    public void activate(String tableName) {
        tx.executeWithoutResult(status -> {
            if (find(tableName).isEmpty()) {
                throw new IllegalArgumentException("Unknown index " + tableName);
            }
            jdbc.update("update index_registry set active = false where active");
            jdbc.update(
                    "update index_registry set active = true, activated_at = now() where table_name = ?", tableName);
        });
        cachedActive = null;
    }
}
