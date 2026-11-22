package ro.iapc.rag.retrieval.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import ro.iapc.rag.testsupport.FakeChatModel;
import ro.iapc.rag.testsupport.FakeModels;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
@Import(FakeModels.class)
class AnswerControllerTest {

    private static final String ONE_CHUNK = """
            {"query":"How often do passwords rotate?","chunks":[
              {"id":"1","docId":"policy","version":"1","source":"policy.pdf","page":3,"chunkIndex":4,
               "text":"Passwords rotate every 90 days.","score":0.82}]}
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    FakeChatModel chatModel;

    @Test
    void answersWithCitationsAndSendsHeadersToTheModel() {
        chatModel.respondWith(prompt -> "Every 90 days [source: policy.pdf, version: 1, page: 3]");

        assertThat(mvc.post()
                        .uri("/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ONE_CHUNK))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.refused").isEqualTo(false);
                    json.assertThat().extractingPath("$.citations[0].source").isEqualTo("policy.pdf");
                    json.assertThat().extractingPath("$.topSimilarity").isEqualTo(0.82);
                });
        assertThat(chatModel.lastPrompt().getContents()).contains("CONTEXT [source: policy.pdf, version: 1, page: 3]");
    }

    @Test
    void refusesWithoutCallingTheModelWhenThereAreNoChunks() {
        int before = chatModel.prompts().size();

        assertThat(mvc.post()
                        .uri("/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"q\",\"chunks\":[]}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.refused")
                .isEqualTo(true);
        assertThat(chatModel.prompts()).hasSize(before);
    }

    @Test
    void modelFailureIsA500() {
        chatModel.failWith(new IllegalStateException("ollama down"));

        assertThat(mvc.post()
                        .uri("/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ONE_CHUNK))
                .hasStatus(500);
    }
}
