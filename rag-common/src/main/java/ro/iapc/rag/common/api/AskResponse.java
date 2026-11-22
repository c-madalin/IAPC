package ro.iapc.rag.common.api;

import java.util.List;
import java.util.Map;

/**
 * @param rankedChunks the chunks the answer was grounded on, in final order with their scores
 * @param topSimilarity best vector similarity among the retrieved chunks, used to justify the refusal threshold
 * @param timingsMs wall time per stage in milliseconds, keyed by stage name
 */
public record AskResponse(
        String answer,
        List<Citation> citations,
        boolean refused,
        List<Chunk> rankedChunks,
        Double topSimilarity,
        String normalizedQuery,
        Map<String, Long> timingsMs) {}
