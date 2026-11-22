package ro.iapc.rag.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTest {

    @LocalServerPort
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void healthIsUp() {
        String body = client().get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void askRouteFailsCleanlyWhenRagApiIsNotRegistered() {
        HttpStatusCode status = client().post()
                .uri("/api/ask")
                .body("{\"query\":\"hi\"}")
                .exchange((request, response) -> response.getStatusCode());
        assertThat(status.value()).isEqualTo(503);
    }
}
