package ro.iapc.rag.eval;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Set on the command line, e.g. {@code --eval.k=5}. See the Makefile's eval target.
 *
 * @param baseUrl the gateway
 * @param retrievalOnly skip /api/ask, which needs the chat model; only recall and MRR are reported
 * @param expand per-request stage flag, null = service default
 */
@ConfigurationProperties("eval")
public record EvalProperties(
        String baseUrl,
        String goldenSet,
        String out,
        int k,
        String pipelineHash,
        String chatModel,
        boolean retrievalOnly,
        Boolean expand,
        Boolean rerank,
        Boolean stitch) {

    public EvalProperties {
        if (baseUrl == null) {
            baseUrl = "http://localhost:8080";
        }
        if (goldenSet == null) {
            goldenSet = "eval/data/golden-set.json";
        }
        if (out == null) {
            out = "eval/reports/latest.json";
        }
        if (k <= 0) {
            k = 5;
        }
        if (pipelineHash == null) {
            pipelineHash = "unknown";
        }
    }
}
