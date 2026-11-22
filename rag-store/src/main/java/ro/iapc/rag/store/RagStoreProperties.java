package ro.iapc.rag.store;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param ollamaBaseUrl where embedding requests go
 * @param defaultModel model of the index ingest-static creates when the registry is empty
 * @param models embedding models allowed for an index, with their vector dimensions. Re-indexing to a model that is
 *     not listed here is refused, which also keeps table names to a known set. The defaults below apply to every
 *     service; add models here rather than in one service's yaml, so all services agree.
 * @param registryCacheTtl how long a service trusts its cached copy of the active index before re-reading it
 */
@ConfigurationProperties("rag.store")
public record RagStoreProperties(
        String ollamaBaseUrl, String defaultModel, Map<String, Integer> models, Duration registryCacheTtl) {

    public RagStoreProperties {
        if (ollamaBaseUrl == null) {
            ollamaBaseUrl = "http://localhost:11434";
        }
        if (defaultModel == null) {
            defaultModel = "nomic-embed-text";
        }
        if (models == null || models.isEmpty()) {
            models = new LinkedHashMap<>();
            models.put("nomic-embed-text", 768);
            models.put("mxbai-embed-large", 1024);
        }
        if (registryCacheTtl == null) {
            registryCacheTtl = Duration.ofSeconds(5);
        }
    }

    public int dimensionsOf(String model) {
        Integer dims = models.get(model);
        if (dims == null) {
            throw new IllegalArgumentException(
                    "Embedding model '" + model + "' is not configured in rag.store.models " + models.keySet());
        }
        return dims;
    }
}
