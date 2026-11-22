package ro.iapc.rag.retrieval.pre;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.store.EmbeddingModelFactory;
import ro.iapc.rag.store.RagStoreProperties;
import ro.iapc.rag.store.VectorStoreProvider;
import ro.iapc.rag.testsupport.FakeEmbeddingModel;
import ro.iapc.rag.testsupport.PgvectorTestcontainer;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
@Import({PgvectorTestcontainer.class, RetrievalControllerIT.Fakes.class})
class RetrievalControllerIT {

    @TestConfiguration
    static class Fakes {
        @Bean
        EmbeddingModelFactory fakeEmbeddings(RagStoreProperties properties) {
            return model -> new FakeEmbeddingModel(properties.dimensionsOf(model));
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    VectorStoreProvider stores;

    @BeforeEach
    void seed() {
        stores.ensureActiveIndex();
        stores.active().delete("doc_id == 'policy'");
        stores.active()
                .add(List.of(
                        chunk(0, "policy: incident response escalation within one hour"),
                        chunk(1, "policy: passwords rotate every ninety days"),
                        chunk(2, "policy: backups are kept for one year")));
    }

    @Test
    void retrievesClosestChunkFirst() {
        assertThat(mvc.post()
                        .uri("/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"  how often do passwords rotate per policy  \",\"topN\":2}"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.chunks.length()").isEqualTo(2);
                    json.assertThat().extractingPath("$.chunks[0].chunkIndex").isEqualTo(1);
                    json.assertThat().extractingPath("$.chunks[0].similarity").isNotNull();
                    json.assertThat()
                            .extractingPath("$.normalizedQuery")
                            .isEqualTo("how often do passwords rotate per policy");
                });
    }

    @Test
    void rejectsBlankQuery() {
        assertThat(mvc.post()
                        .uri("/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\" \"}"))
                .hasStatus(400);
    }

    @Test
    void rejectsFiltersUntilStory3Task1() {
        assertThat(mvc.post()
                        .uri("/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\",\"rawFilters\":{\"source\":[\"a.pdf\"]}}"))
                .hasStatus(400);
    }

    @Test
    void returnsNeighborsInOrder() {
        assertThat(mvc.get().uri("/chunks/neighbors?docId=policy&index=1&window=1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.chunks[*].chunkIndex")
                .isEqualTo(List.of(0, 1, 2));
    }

    private static Document chunk(int index, String text) {
        return Document.builder()
                .text(text)
                .metadata(Map.of(
                        MetadataKeys.DOC_ID, "policy",
                        MetadataKeys.VERSION, "1",
                        MetadataKeys.SOURCE, "policy.pdf",
                        MetadataKeys.PAGE, 1,
                        MetadataKeys.CHUNK_INDEX, index))
                .build();
    }
}
