package ro.iapc.rag.common.api;

import java.util.List;

/** Candidates ordered by similarity, highest first. */
public record RetrieveResponse(List<Chunk> chunks, String normalizedQuery) {}
