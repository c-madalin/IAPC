package ro.iapc.rag.ingestdynamic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SourceController.class)
class SourceControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void sourceIngestionIsNotImplementedYet() {
        assertThat(mvc.post()
                        .uri("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org\"}"))
                .hasStatus(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void webhookIsNotImplementedYet() {
        assertThat(mvc.post().uri("/webhooks/vendor").content("{}")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
    }
}
