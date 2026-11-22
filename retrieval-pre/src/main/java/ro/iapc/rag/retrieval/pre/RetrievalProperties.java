package ro.iapc.rag.retrieval.pre;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param defaultTopN candidates returned when the request doesn't say
 * @param maxTopN upper bound on a requested topN
 */
@ConfigurationProperties("rag.retrieval")
public record RetrievalProperties(int defaultTopN, int maxTopN) {

    public RetrievalProperties {
        if (defaultTopN <= 0) {
            defaultTopN = 20;
        }
        if (maxTopN <= 0) {
            maxTopN = 100;
        }
    }
}
