package ro.iapc.rag.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;

public class OllamaEmbeddingModelFactory implements EmbeddingModelFactory {

    private final OllamaApi ollamaApi;
    private final Map<String, EmbeddingModel> models = new ConcurrentHashMap<>();

    public OllamaEmbeddingModelFactory(String baseUrl) {
        this.ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
    }

    @Override
    public EmbeddingModel forModel(String model) {
        return models.computeIfAbsent(
                model,
                name -> OllamaEmbeddingModel.builder()
                        .ollamaApi(ollamaApi)
                        .options(OllamaEmbeddingOptions.builder().model(name).build())
                        .build());
    }
}
