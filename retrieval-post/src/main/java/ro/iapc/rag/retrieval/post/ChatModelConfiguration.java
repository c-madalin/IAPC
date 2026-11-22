package ro.iapc.rag.retrieval.post;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ChatModelConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    ChatModel ollamaChatModel(AnswerProperties properties) {
        return OllamaChatModel.builder()
                .ollamaApi(
                        OllamaApi.builder().baseUrl(properties.ollamaBaseUrl()).build())
                .options(OllamaChatOptions.builder()
                        .model(properties.chatModel())
                        .temperature(0.0)
                        .build())
                .build();
    }
}
