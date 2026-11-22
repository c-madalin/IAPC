package ro.iapc.rag.testsupport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Deterministic embedding for tests: each lower-cased word is hashed into one of {@code dimensions} buckets and the
 * vector is L2-normalised. Texts that share words get a high cosine similarity, so retrieval tests behave sensibly
 * without Ollama.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public FakeEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    public FakeEmbeddingModel() {
        this(768);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(embed(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText() == null ? "" : document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty()) {
                continue;
            }
            CRC32 crc = new CRC32();
            crc.update(word.getBytes(StandardCharsets.UTF_8));
            vector[(int) (crc.getValue() % dimensions)] += 1f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm == 0) {
            vector[0] = 1f;
            return vector;
        }
        float inv = (float) (1 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= inv;
        }
        return vector;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }
}
