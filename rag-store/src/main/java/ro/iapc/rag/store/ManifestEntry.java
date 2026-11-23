package ro.iapc.rag.store;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * One row of {@code ingestion_manifest}: one document version in one vector table.
 *
 * @param ingestedAt set by the database; ignored on write
 */
public record ManifestEntry(
        String docId,
        String version,
        String source,
        SourceType sourceType,
        String checksum,
        String embeddingModel,
        String tableName,
        int chunkCount,
        OffsetDateTime ingestedAt) {

    /** Which ingestion service wrote the document. Stored in lowercase. */
    public enum SourceType {
        STATIC,
        DYNAMIC;

        String column() {
            return name().toLowerCase(Locale.ROOT);
        }

        static SourceType fromColumn(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
