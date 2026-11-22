package ro.iapc.rag.ingeststatic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import ro.iapc.rag.store.ChunkQueries;
import ro.iapc.rag.store.EmbeddingModelFactory;
import ro.iapc.rag.store.RagStoreProperties;
import ro.iapc.rag.store.VectorStoreProvider;
import ro.iapc.rag.testsupport.FakeEmbeddingModel;
import ro.iapc.rag.testsupport.PgvectorTestcontainer;

@SpringBootTest(properties = {"eureka.client.enabled=false", "rag.ingest.resources=classpath:pdfs/*.pdf"})
@AutoConfigureMockMvc
@Import({PgvectorTestcontainer.class, DocumentControllerIT.Fakes.class})
class DocumentControllerIT {

    @TestConfiguration
    static class Fakes {
        @Bean
        EmbeddingModelFactory fakeEmbeddings(RagStoreProperties properties) {
            return model -> new FakeEmbeddingModel(properties.dimensionsOf(model));
        }
    }

    private static final String DOC_ID = "static:scope_roles.pdf";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    VectorStoreProvider stores;

    @Autowired
    ChunkQueries chunkQueries;

    @Test
    void syncIngestsWithFullMetadataAndRerunCreatesNoDuplicates() {
        assertThat(mvc.post().uri("/documents/sync")).hasStatusOk();
        int first = chunkQueries.countByDocId(stores.activeIndex(), DOC_ID);
        assertThat(first).isPositive();

        assertThat(mvc.post().uri("/documents/sync")).hasStatusOk();
        assertThat(chunkQueries.countByDocId(stores.activeIndex(), DOC_ID)).isEqualTo(first);

        var chunk = chunkQueries
                .byIndexRange(stores.activeIndex(), DOC_ID, "1", 0, 0)
                .getFirst();
        assertThat(chunk.source()).isEqualTo("scope_roles.pdf");
        assertThat(chunk.page()).isNotNull();
        assertThat(chunk.chunkIndex()).isZero();

        assertThat(mvc.get().uri("/manifest"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].chunkCount")
                .isEqualTo(first);
    }

    @Test
    void deleteRemovesAllChunksAndUnknownDocIs404() {
        mvc.post().uri("/documents/sync").exchange();

        assertThat(mvc.delete().uri("/documents/{id}", DOC_ID)).hasStatusOk();
        assertThat(chunkQueries.countByDocId(stores.activeIndex(), DOC_ID)).isZero();

        assertThat(mvc.delete().uri("/documents/{id}", "static:missing.pdf")).hasStatus(404);
    }
}
