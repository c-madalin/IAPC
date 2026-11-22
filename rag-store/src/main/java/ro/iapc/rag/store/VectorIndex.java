package ro.iapc.rag.store;

/** One vector table, built with one embedding model. Exactly one index is active at a time. */
public record VectorIndex(String tableName, String embeddingModel, int dimensions, boolean active) {}
