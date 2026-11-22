package ro.iapc.rag.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.RestClient;
import ro.iapc.rag.platform.ServiceClients;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
@Import(AskControllerTest.MockedServices.class)
class AskControllerTest {

    @TestConfiguration
    static class MockedServices {
        static final RestClient.Builder BUILDER = RestClient.builder();
        static final MockRestServiceServer SERVER =
                MockRestServiceServer.bindTo(BUILDER).build();

        @Bean
        @Primary
        ServiceClients mockedServiceClients() {
            return new ServiceClients(
                    BUILDER, BUILDER, Map.of("retrieval-pre", "http://pre", "retrieval-post", "http://post"));
        }
    }

    private static final String CHUNKS = """
            {"chunks":[{"id":"1","docId":"policy","version":"1","source":"policy.pdf","page":3,"chunkIndex":0,
              "text":"Passwords rotate every 90 days.","score":0.8}],"normalizedQuery":"passwords"}
            """;

    private static final String ANSWER = """
            {"answer":"Every 90 days","citations":[{"docId":"policy","source":"policy.pdf","version":"1","page":3}],
             "refused":false,"rankedChunks":[],"topSimilarity":0.8}
            """;

    @Autowired
    MockMvcTester mvc;

    @BeforeEach
    void reset() {
        MockedServices.SERVER.reset();
    }

    @Test
    void chainsPreThenPostAndForwardsFilters() {
        MockedServices.SERVER
                .expect(requestTo("http://pre/retrieve"))
                .andExpect(jsonPath("$.rawFilters.source[0]").value("policy.pdf"))
                .andRespond(withSuccess(CHUNKS, MediaType.APPLICATION_JSON));
        MockedServices.SERVER
                .expect(requestTo("http://post/answer"))
                .andExpect(jsonPath("$.chunks[0].docId").value("policy"))
                .andRespond(withSuccess(ANSWER, MediaType.APPLICATION_JSON));

        assertThat(mvc.post()
                        .uri("/api/ask?source=policy.pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"How often do passwords rotate?\"}"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.answer").isEqualTo("Every 90 days");
                    json.assertThat().extractingPath("$.normalizedQuery").isEqualTo("passwords");
                    json.assertThat().extractingPath("$.timingsMs.pre").isNotNull();
                });
        MockedServices.SERVER.verify();
    }

    @Test
    void retrievalFailureBecomes502() {
        MockedServices.SERVER.expect(requestTo("http://pre/retrieve")).andRespond(withServerError());

        assertThat(mvc.post()
                        .uri("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"q\"}"))
                .hasStatus(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void clientErrorsFromAStagePassThrough() {
        MockedServices.SERVER
                .expect(requestTo("http://pre/retrieve"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"detail\":\"unknown field\"}"));

        assertThat(mvc.post()
                        .uri("/api/ask?nope=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"q\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blankQueryIs400() {
        assertThat(mvc.post()
                        .uri("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
