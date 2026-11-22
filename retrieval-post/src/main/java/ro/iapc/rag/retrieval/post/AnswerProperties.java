package ro.iapc.rag.retrieval.post;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param ollamaBaseUrl Ollama for the chat model
 * @param chatModel model name, e.g. qwen2.5:7b
 * @param maxContextChunks how many chunks go into the prompt
 */
@ConfigurationProperties("rag.answer")
public record AnswerProperties(String ollamaBaseUrl, String chatModel, int maxContextChunks) {

    public AnswerProperties {
        if (ollamaBaseUrl == null) {
            ollamaBaseUrl = "http://localhost:11434";
        }
        if (chatModel == null) {
            chatModel = "qwen2.5:7b";
        }
        if (maxContextChunks <= 0) {
            maxContextChunks = 5;
        }
    }
}
