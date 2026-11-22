package ro.iapc.rag.common.api;

import java.util.List;

/** Body of {@code POST /answer} on retrieval-post. {@code chunks} come from retrieval-pre, best first. */
public record AnswerRequest(String query, List<Chunk> chunks, Stages stages) {}
