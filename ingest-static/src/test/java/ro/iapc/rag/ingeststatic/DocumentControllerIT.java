package ro.iapc.rag.ingeststatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Optional;
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
import ro.iapc.rag.store.ManifestEntry;
import ro.iapc.rag.store.ManifestRepository;
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

    @Autowired
    ManifestRepository manifest;

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

        assertThat(mvc.delete().uri("/documents/{id}", DOC_ID))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.orphanedVectors")
                .isEqualTo(0);
        assertThat(chunkQueries.countByDocId(stores.activeIndex(), DOC_ID)).isZero();

        assertThat(mvc.delete().uri("/documents/{id}", "static:missing.pdf")).hasStatus(404);
    }

    @Test
    void unchangedSyncReturnsTheExistingManifestEntry() {
        assertThat(mvc.post().uri("/documents/sync")).hasStatusOk();
        ManifestEntry first =
                manifest.findLatest(DOC_ID, stores.activeIndex().tableName()).orElseThrow();

        assertThat(mvc.post().uri("/documents/sync")).hasStatusOk();

        assertThat(manifest.findLatest(DOC_ID, stores.activeIndex().tableName()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(first);
    }

    @Test
    void computesTheNextStaticVersion() {
        ManifestEntry existing = new ManifestEntry(
                "static:a.pdf",
                "4",
                "a.pdf",
                ManifestEntry.SourceType.STATIC,
                "abc",
                "nomic-embed-text",
                "vector_store_nomic_embed_text",
                3,
                OffsetDateTime.now());

        assertThat(PdfIngestionService.nextVersion(Optional.of(existing))).isEqualTo("5");
        assertThat(PdfIngestionService.nextVersion(Optional.empty())).isEqualTo("1");

        ManifestEntry invalid = new ManifestEntry(
                "static:a.pdf",
                "latest",
                "a.pdf",
                ManifestEntry.SourceType.STATIC,
                "abc",
                "nomic-embed-text",
                "vector_store_nomic_embed_text",
                3,
                OffsetDateTime.now());
        assertThatThrownBy(() -> PdfIngestionService.nextVersion(Optional.of(invalid)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
