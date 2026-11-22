package ro.iapc.rag.common.api;

import java.util.List;

public record AnswerResponse(
        String answer, List<Citation> citations, boolean refused, List<Chunk> rankedChunks, Double topSimilarity) {}
