package ro.iapc.rag.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import ro.iapc.rag.common.api.AskRequest;
import ro.iapc.rag.common.api.AskResponse;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.common.api.Citation;
import ro.iapc.rag.common.api.RetrieveRequest;
import ro.iapc.rag.common.api.RetrieveResponse;
import ro.iapc.rag.common.api.Stages;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the golden set through the gateway. Retrieval metrics come from retrieval-pre directly so they measure
 * retrieval alone; refusal and citation checks come from the full /api/ask pipeline.
 */
public class EvalRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);

    private final RestClient client;
    private final EvalProperties props;
    private final JsonMapper json =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public EvalRunner(RestClient client, EvalProperties props) {
        this.client = client;
        this.props = props;
    }

    public EvalReport run() throws Exception {
        GoldenSet golden = json.readValue(Path.of(props.goldenSet()).toFile(), GoldenSet.class);
        Stages stages = new Stages(props.expand(), props.rerank(), props.stitch());
        List<EvalReport.QuestionResult> results = new ArrayList<>();

        for (GoldenSet.Question q : golden.questions()) {
            results.add(evaluate(q, stages));
        }

        EvalReport report = new EvalReport(
                props.pipelineHash(),
                Instant.now().toString(),
                props.retrievalOnly() ? "none (retrieval only)" : props.chatModel(),
                props.k(),
                stagesMap(stages),
                summarize(results),
                results);
        Path out = Path.of(props.out());
        Files.createDirectories(out.toAbsolutePath().getParent());
        json.writeValue(out.toFile(), report);
        log.info("Wrote {} with metrics {}", out, report.metrics());
        return report;
    }

    private EvalReport.QuestionResult evaluate(GoldenSet.Question q, Stages stages) {
        try {
            RetrieveResponse retrieved = client.post()
                    .uri("/retrieval-pre/retrieve")
                    .body(new RetrieveRequest(q.question(), Map.of(), null, stages))
                    .retrieve()
                    .body(RetrieveResponse.class);
            List<String> ranked = Metrics.distinctInOrder(
                    retrieved.chunks().stream().map(Chunk::docId).toList());
            boolean hit = q.answerable() && Metrics.hitAtK(ranked, q.expectedDocIds(), props.k());
            double rr = q.answerable() ? Metrics.reciprocalRank(ranked, q.expectedDocIds(), props.k()) : 0.0;

            if (props.retrievalOnly()) {
                return new EvalReport.QuestionResult(
                        q.id(), q.answerable(), ranked, hit, rr, null, null, null, null, null);
            }
            AskResponse answer = client.post()
                    .uri("/api/ask")
                    .body(new AskRequest(q.question(), stages))
                    .retrieve()
                    .body(AskResponse.class);
            return new EvalReport.QuestionResult(
                    q.id(),
                    q.answerable(),
                    ranked,
                    hit,
                    rr,
                    answer.refused(),
                    answer.topSimilarity(),
                    citationsGrounded(answer),
                    answer.answer(),
                    null);
        } catch (RuntimeException e) {
            log.error("Question {} failed", q.id(), e);
            return new EvalReport.QuestionResult(
                    q.id(), q.answerable(), List.of(), false, 0.0, null, null, null, null, e.getMessage());
        }
    }

    /**
     * Basic faithfulness: every cited document must be among the chunks the answer was grounded on. TODO(story-4-task-4):
     * add a stronger check (e.g. an LLM judge verifying each claim against the context).
     */
    static boolean citationsGrounded(AskResponse answer) {
        if (answer.citations() == null || answer.citations().isEmpty()) {
            return answer.refused();
        }
        Set<String> contextDocs = answer.rankedChunks() == null
                ? Set.of()
                : answer.rankedChunks().stream().map(Chunk::docId).collect(Collectors.toSet());
        return answer.citations().stream().map(Citation::docId).allMatch(contextDocs::contains);
    }

    Map<String, Double> summarize(List<EvalReport.QuestionResult> results) {
        List<EvalReport.QuestionResult> answerable =
                results.stream().filter(EvalReport.QuestionResult::answerable).toList();
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put(
                "recall_at_k",
                Metrics.mean(answerable.stream().map(r -> r.hit() ? 1.0 : 0.0).toList()));
        metrics.put(
                "mrr",
                Metrics.mean(answerable.stream()
                        .map(EvalReport.QuestionResult::reciprocalRank)
                        .toList()));
        if (!props.retrievalOnly()) {
            metrics.put(
                    "refusal_accuracy",
                    Metrics.mean(results.stream()
                            .filter(r -> r.refused() != null)
                            .map(r -> r.refused() == !r.answerable() ? 1.0 : 0.0)
                            .toList()));
            metrics.put(
                    "citation_faithfulness",
                    Metrics.mean(results.stream()
                            .filter(r -> r.citationsGrounded() != null)
                            .map(r -> r.citationsGrounded() ? 1.0 : 0.0)
                            .toList()));
        }
        metrics.put("errors", (double)
                results.stream().filter(r -> r.error() != null).count());
        return metrics;
    }

    private static Map<String, Object> stagesMap(Stages s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expand", s.expand());
        m.put("rerank", s.rerank());
        m.put("stitch", s.stitch());
        return m;
    }
}
