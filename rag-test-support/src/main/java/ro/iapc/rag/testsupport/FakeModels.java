package ro.iapc.rag.testsupport;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces Ollama with {@link FakeChatModel} and {@link FakeEmbeddingModel}. Use with {@code @Import(FakeModels.class)}.
 * Inject {@code ChatModel} or {@code FakeChatModel}; both resolve to the same fake. Services built on rag-store also
 * declare {@code @Bean EmbeddingModelFactory} returning {@code model -> new FakeEmbeddingModel(dims)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeModels {

    @Bean
    @Primary
    public FakeChatModel fakeChatModel() {
        return new FakeChatModel();
    }

    @Bean
    @Primary
    public EmbeddingModel fakeEmbeddingModel() {
        return new FakeEmbeddingModel();
    }
}
