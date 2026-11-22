package ro.iapc.rag.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres with pgvector for integration tests, wired as the DataSource through {@code @ServiceConnection}. Use with
 * {@code @Import(PgvectorTestcontainer.class)}. The image matches compose.yaml.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PgvectorTestcontainer {

    public static final DockerImageName IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    public PostgreSQLContainer pgvector() {
        return new PostgreSQLContainer(IMAGE);
    }
}
