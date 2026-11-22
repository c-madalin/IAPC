package ro.iapc.rag.testsupport;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Chat model for tests. Answers with a fixed string or a function of the prompt, and keeps every prompt it received
 * so tests can assert on what was sent.
 */
public class FakeChatModel implements ChatModel {

    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private volatile Function<Prompt, String> responder;

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

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(responder.apply(prompt)))));
    }
}
