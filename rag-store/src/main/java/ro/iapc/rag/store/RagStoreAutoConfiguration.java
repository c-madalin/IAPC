package ro.iapc.rag.store;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(RagStoreProperties.class)
public class RagStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EmbeddingModelFactory embeddingModelFactory(RagStoreProperties properties) {
        return new OllamaEmbeddingModelFactory(properties.ollamaBaseUrl());
    }

    @Bean
    @ConditionalOnMissingBean
    IndexRegistry indexRegistry(JdbcTemplate jdbc, TransactionTemplate tx, RagStoreProperties properties) {
        return new IndexRegistry(jdbc, tx, properties, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    VectorStoreProvider vectorStoreProvider(
            JdbcTemplate jdbc,
            IndexRegistry registry,
            EmbeddingModelFactory embeddingModels,
            RagStoreProperties properties) {
        return new VectorStoreProvider(jdbc, registry, embeddingModels, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ChunkQueries chunkQueries(JdbcTemplate jdbc) {
        return new ChunkQueries(jdbc);
    }
}
