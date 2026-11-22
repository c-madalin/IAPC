package ro.iapc.rag.store;

import org.springframework.ai.embedding.EmbeddingModel;

/** Gives the embedding model for a model name. Tests replace the Ollama-backed bean with a fake. */
public interface EmbeddingModelFactory {

    EmbeddingModel forModel(String model);
}
