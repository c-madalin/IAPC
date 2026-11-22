package ro.iapc.rag.store;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ro.iapc.rag.testsupport.FakeEmbeddingModel;

/** Minimal app so rag-store can be tested with its auto-configuration and Flyway. */
@SpringBootApplication
class TestStoreApplication {

    @Bean
    EmbeddingModelFactory fakeEmbeddings(RagStoreProperties properties) {
        return model -> new FakeEmbeddingModel(properties.dimensionsOf(model));
    }
}
