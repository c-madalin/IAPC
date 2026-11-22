package ro.iapc.rag.common.api;

import java.util.List;

/** Result of {@code GET /chunks/neighbors} on retrieval-pre, ordered by {@code chunkIndex}. */
public record NeighborsResponse(List<Chunk> chunks) {}
