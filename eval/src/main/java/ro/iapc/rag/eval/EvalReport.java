package ro.iapc.rag.eval;

import java.util.List;
import java.util.Map;

/**
 * Written to eval/reports/latest.json. CI compares {@code pipelineHash} with the hash of the current pipeline sources
 * and the metrics with eval/baseline.json.
 *
 * @param metrics recall_at_k, mrr, refusal_accuracy, citation_faithfulness (0..1)
 */
public record EvalReport(
        String pipelineHash,
        String createdAt,
        String chatModel,
        int k,
        Map<String, Object> stages,
        Map<String, Double> metrics,
        List<QuestionResult> results) {

    public record QuestionResult(
            String id,
            boolean answerable,
            List<String> retrievedDocIds,
            boolean hit,
            double reciprocalRank,
            Boolean refused,
            Double topSimilarity,
            Boolean citationsGrounded,
            String answer,
            String error) {}
}
