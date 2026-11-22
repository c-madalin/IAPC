package ro.iapc.rag.eval;

import java.util.List;

/** Retrieval metrics over ranked doc ids. A question counts as a hit if any expected doc appears in the top k. */
public final class Metrics {

    private Metrics() {}

    public static boolean hitAtK(List<String> rankedDocIds, List<String> expected, int k) {
        return rankedDocIds.stream().limit(k).anyMatch(expected::contains);
    }

    /** 1/rank of the first relevant doc in the top k, 0 if none. */
    public static double reciprocalRank(List<String> rankedDocIds, List<String> expected, int k) {
        for (int i = 0; i < Math.min(k, rankedDocIds.size()); i++) {
            if (expected.contains(rankedDocIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /** Ranked list with repeated doc ids removed, since several chunks of one doc are one hit. */
    public static List<String> distinctInOrder(List<String> docIds) {
        return docIds.stream().distinct().toList();
    }

    public static double mean(List<Double> values) {
        return values.isEmpty()
                ? 0.0
                : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
