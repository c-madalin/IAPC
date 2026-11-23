package ro.iapc.rag.store;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ro.iapc.rag.common.MetadataKeys;

/**
 * Writes a new version of a document so that queries never see a mix of versions or no chunks at all, and a failure
 * leaves the previous version in place.
 *
 * <p>Two steps. Staging adds the chunks hidden: {@code is_latest=false}, and the version under {@code staged_version}
 * instead of {@code version}, so neither the default {@code is_latest} search nor a version filter matches them. This
 * is where the embedding calls happen, outside any transaction. Publishing then retires the old chunks and reveals
 * the new ones in one short transaction.
 *
 * <p>A crash between the steps leaves staged rows that nothing reads. They still carry {@code doc_id}, so deleting
 * the document removes them, and an orphan check finds them by {@code staged_version}.
 */
public class ChunkWriter {

    private static final Logger log = LoggerFactory.getLogger(ChunkWriter.class);

    /** What happens to the document's current chunks when the new ones are published. */
    public enum Retire {
        /** Delete them. Static documents keep one version. */
        DELETE,
        /** Keep them with {@code is_latest=false}, so explicit version queries still find them (Story 2 Task 2). */
        FLAG
    }

    /** @param retired chunks deleted or flagged; 0 for a new document */
    public record Result(String docId, String version, int published, int retired) {}

    private final VectorStoreProvider stores;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ChunkWriter(VectorStoreProvider stores, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.stores = stores;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /**
     * Writes {@code chunks} as the latest version of {@code docId}. Every chunk must carry that {@code doc_id} and the
     * same {@code version}.
     */
    public Result write(VectorIndex index, String docId, List<Document> chunks, Retire retire) {
        String version = stage(index, docId, chunks);
        Result result = publish(index, docId, version, ids(chunks), retire);
        // DEBUG because the calling service logs the ingestion itself, with the counts from Result.
        log.debug(
                "Published {} chunks of {} version {} into {}, {} old chunks {}",
                result.published(),
                docId,
                version,
                index.tableName(),
                result.retired(),
                retire == Retire.DELETE ? "deleted" : "flagged");
        return result;
    }

    /** Adds the chunks hidden and returns their version. On failure, removes whatever it added. */
    String stage(VectorIndex index, String docId, List<Document> chunks) {
        String version = checkedVersion(docId, chunks);
        for (Document chunk : chunks) {
            var metadata = chunk.getMetadata();
            metadata.remove(MetadataKeys.VERSION);
            metadata.put(MetadataKeys.STAGED_VERSION, version);
            metadata.put(MetadataKeys.IS_LATEST, false);
        }
        try {
            stores.forIndex(index).add(chunks);
        } catch (RuntimeException e) {
            removeStaged(index, ids(chunks), e);
            throw e;
        } finally {
            for (Document chunk : chunks) {
                var metadata = chunk.getMetadata();
                metadata.remove(MetadataKeys.STAGED_VERSION);
                metadata.put(MetadataKeys.VERSION, version);
                metadata.put(MetadataKeys.IS_LATEST, true);
            }
        }
        return version;
    }

    /** Retires the current chunks and reveals the staged ones, in one transaction. */
    Result publish(VectorIndex index, String docId, String version, List<String> ids, Retire retire) {
        String table = index.tableName();
        String current = " where metadata->>'" + MetadataKeys.DOC_ID + "' = ?"
                + " and metadata->>'" + MetadataKeys.STAGED_VERSION + "' is null"
                + " and metadata->>'" + MetadataKeys.IS_LATEST + "' = 'true'";
        String retireSql = retire == Retire.DELETE
                ? "delete from " + table + " where metadata->>'" + MetadataKeys.DOC_ID + "' = ?" + " and metadata->>'"
                        + MetadataKeys.STAGED_VERSION + "' is null"
                : "update " + table + " set metadata = jsonb_set(metadata::jsonb, '{" + MetadataKeys.IS_LATEST
                        + "}', 'false')::json" + current;
        String publishSql = "update " + table + " set metadata = ((metadata::jsonb - '" + MetadataKeys.STAGED_VERSION
                + "') || jsonb_build_object('" + MetadataKeys.VERSION + "', metadata->>'"
                + MetadataKeys.STAGED_VERSION + "', '" + MetadataKeys.IS_LATEST + "', true))::json"
                + " where id = any(?::uuid[]) and metadata->>'" + MetadataKeys.STAGED_VERSION + "' = ?";
        return tx.execute(status -> {
            int retired = jdbc.update(retireSql, docId);
            int published = jdbc.update(con -> {
                var ps = con.prepareStatement(publishSql);
                ps.setArray(1, con.createArrayOf("uuid", ids.toArray()));
                ps.setString(2, version);
                return ps;
            });
            if (published != ids.size()) {
                // Rolls back the retire too, so the previous version stays.
                throw new IllegalStateException(
                        "Expected to publish " + ids.size() + " staged chunks of " + docId + " but found " + published);
            }
            return new Result(docId, version, published, retired);
        });
    }

    private void removeStaged(VectorIndex index, List<String> ids, RuntimeException cause) {
        try {
            stores.forIndex(index).delete(ids);
        } catch (RuntimeException cleanup) {
            cause.addSuppressed(cleanup);
        }
    }

    private static String checkedVersion(String docId, List<Document> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("No chunks for " + docId + "; delete the document instead");
        }
        String version = Objects.toString(chunks.getFirst().getMetadata().get(MetadataKeys.VERSION), null);
        if (version == null) {
            throw new IllegalArgumentException("Chunks of " + docId + " have no " + MetadataKeys.VERSION);
        }
        for (Document chunk : chunks) {
            var metadata = chunk.getMetadata();
            if (!docId.equals(metadata.get(MetadataKeys.DOC_ID))
                    || !version.equals(Objects.toString(metadata.get(MetadataKeys.VERSION), null))) {
                throw new IllegalArgumentException("Every chunk must have " + MetadataKeys.DOC_ID + " " + docId
                        + " and " + MetadataKeys.VERSION + " " + version);
            }
        }
        return version;
    }

    private static List<String> ids(List<Document> chunks) {
        return chunks.stream().map(Document::getId).toList();
    }
}
