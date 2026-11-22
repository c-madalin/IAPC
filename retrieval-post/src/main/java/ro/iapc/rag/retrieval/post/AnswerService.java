package ro.iapc.rag.retrieval.post;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import ro.iapc.rag.common.MetricNames;
import ro.iapc.rag.common.api.AnswerRequest;
import ro.iapc.rag.common.api.AnswerResponse;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.common.api.Citation;
import ro.iapc.rag.platform.StageTimer;

/**
 * Walking-skeleton answer: top chunks into a prompt, one LLM call. Story 4 adds reranking (Task 1), neighbor stitching
 * (Task 2), and the grounded prompt with a similarity threshold (Task 3).
 */
@Service
public class AnswerService {

    static final String NO_ANSWER = "I don't know.";

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private static final String SYSTEM_PROMPT = """
            You answer questions about internal documents.
            Use only the CONTEXT blocks below. If they don't contain the answer, reply exactly: I don't know.
            Cite the blocks you used by their header, for example [source: policy.pdf, version: 1, page: 3].
            """;

    private final ChatModel chatModel;
    private final AnswerProperties properties;
    private final StageTimer timer;
    private final MeterRegistry meters;

    public AnswerService(ChatModel chatModel, AnswerProperties properties, StageTimer timer, MeterRegistry meters) {
        this.chatModel = chatModel;
        this.properties = properties;
        this.timer = timer;
        this.meters = meters;
    }

    public AnswerResponse answer(AnswerRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        List<Chunk> chunks = request.chunks() == null ? List.of() : request.chunks();
        Double topSimilarity = chunks.stream()
                .map(Chunk::similarity)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(null);
        if (chunks.isEmpty()) {
            log.info("No chunks for query, refusing");
            meters.counter(MetricNames.REFUSALS).increment();
            return new AnswerResponse(NO_ANSWER, List.of(), true, List.of(), null);
        }

        List<Chunk> context =
                chunks.stream().limit(properties.maxContextChunks()).toList();
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(contextBlock(context) + "\nQUESTION: " + request.query())));
        ChatResponse response = timer.time("llm", () -> chatModel.call(prompt));
        String answer = response.getResult().getOutput().getText();
        boolean refused = answer == null || answer.strip().startsWith("I don't know");
        recordUsage(response, refused);
        log.info("Answered from {} chunks, refused={}", context.size(), refused);
        List<Citation> citations = refused
                ? List.of()
                : context.stream().map(Citation::of).distinct().toList();
        return new AnswerResponse(answer, citations, refused, context, topSimilarity);
    }

    private void recordUsage(ChatResponse response, boolean refused) {
        if (refused) {
            meters.counter(MetricNames.REFUSALS).increment();
        }
        var usage =
                response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (usage != null) {
            if (usage.getPromptTokens() != null) {
                meters.counter(MetricNames.LLM_TOKENS, "type", "prompt").increment(usage.getPromptTokens());
            }
            if (usage.getCompletionTokens() != null) {
                meters.counter(MetricNames.LLM_TOKENS, "type", "completion").increment(usage.getCompletionTokens());
            }
        }
    }

    static String contextBlock(List<Chunk> chunks) {
        return chunks.stream()
                .map(c -> "CONTEXT [source: %s, version: %s, page: %s]%n%s%n"
                        .formatted(c.source(), c.version(), c.page(), c.text()))
                .collect(Collectors.joining("\n"));
    }
}
