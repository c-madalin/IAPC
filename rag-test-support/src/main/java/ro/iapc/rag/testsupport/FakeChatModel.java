package ro.iapc.rag.testsupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Chat model for tests. Answers with a fixed string or a function of the prompt, and keeps every prompt it received
 * so tests can assert on what was sent.
 *
 * <p>Every response carries token usage, like Ollama's: by default the number of whitespace-separated words in the
 * prompt and in the answer. {@link #reportUsage} fixes the numbers, {@link #reportNoUsage} leaves usage out.
 */
public class FakeChatModel implements ChatModel {

    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private volatile Function<Prompt, String> responder;
    private volatile BiFunction<Prompt, String, Usage> usage = FakeChatModel::wordCounts;

    public FakeChatModel(String fixedAnswer) {
        this.responder = prompt -> fixedAnswer;
    }

    public FakeChatModel() {
        this("fake answer");
    }

    public void respondWith(Function<Prompt, String> responder) {
        this.responder = responder;
    }

    public void failWith(RuntimeException error) {
        this.responder = prompt -> {
            throw error;
        };
    }

    public void reportUsage(int promptTokens, int completionTokens) {
        this.usage = (prompt, answer) -> new DefaultUsage(promptTokens, completionTokens);
    }

    /**
     * For testing code that has to cope with a model that reports no usage. Spring AI then returns an empty usage
     * whose counts are 0.
     */
    public void reportNoUsage() {
        this.usage = (prompt, answer) -> null;
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        String answer = responder.apply(prompt);
        Usage reported = usage.apply(prompt, answer);
        List<Generation> generations = List.of(new Generation(new AssistantMessage(answer)));
        if (reported == null) {
            return new ChatResponse(generations);
        }
        return new ChatResponse(
                generations, ChatResponseMetadata.builder().usage(reported).build());
    }

    private static Usage wordCounts(Prompt prompt, String answer) {
        return new DefaultUsage(words(prompt.getContents()), words(answer));
    }

    private static int words(String text) {
        return text == null || text.isBlank() ? 0 : text.strip().split("\\s+").length;
    }
}
