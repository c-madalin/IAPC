package ro.iapc.rag.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.iapc.rag.common.api.AskResponse;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.common.api.Citation;

class MetricsTest {

    private static final List<String> RANKED = List.of("a", "b", "c");

    @Test
    void reciprocalRankUsesFirstRelevantPosition() {
        assertThat(Metrics.reciprocalRank(RANKED, List.of("b"), 5)).isEqualTo(0.5);
        assertThat(Metrics.reciprocalRank(RANKED, List.of("c", "a"), 5)).isEqualTo(1.0);
    }

    @Test
    void missOutsideTopKScoresZero() {
        assertThat(Metrics.hitAtK(RANKED, List.of("c"), 2)).isFalse();
        assertThat(Metrics.reciprocalRank(RANKED, List.of("c"), 2)).isZero();
        assertThat(Metrics.reciprocalRank(List.of(), List.of("a"), 5)).isZero();
    }

    @Test
    void duplicateDocIdsCollapse() {
        assertThat(Metrics.distinctInOrder(List.of("a", "a", "b", "a"))).containsExactly("a", "b");
    }

    @Test
    void citationOutsideContextIsNotGrounded() {
        Chunk chunk = new Chunk("1", "doc-a", "1", "a.pdf", 1, 0, "text", 0.9);
        AskResponse grounded = new AskResponse(
                "x", List.of(new Citation("doc-a", "a.pdf", "1", 1)), false, List.of(chunk), 0.9, "q", null);
        AskResponse invented = new AskResponse(
                "x", List.of(new Citation("doc-z", "z.pdf", "1", 1)), false, List.of(chunk), 0.9, "q", null);

        assertThat(EvalRunner.citationsGrounded(grounded)).isTrue();
        assertThat(EvalRunner.citationsGrounded(invented)).isFalse();
    }
}
