package ro.iapc.rag.ingeststatic;

import java.time.OffsetDateTime;

public record ManifestEntry(
        String docId,
        String version,
        String source,
        String checksum,
        String embeddingModel,
        String tableName,
        int chunkCount,
        OffsetDateTime ingestedAt) {}
