package ro.iapc.rag.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.testsupport.PgvectorTestcontainer;

@SpringBootTest(classes = TestStoreApplication.class)
@Import(PgvectorTestcontainer.class)
@DirtiesContext
class VectorStoreProviderIT {

    @Autowired
    VectorStoreProvider provider;

    @Autowired
    IndexRegistry registry;

    @Autowired
    ChunkQueries queries;

    @Test
    void createsDefaultIndexSearchesAndSwitchesToAnotherModel() {
        VectorIndex first = provider.ensureActiveIndex();
        assertThat(first.tableName()).isEqualTo("vector_store_nomic_embed_text");
        assertThat(first.dimensions()).isEqualTo(768);

        provider.active()
                .add(List.of(
                        chunk("doc-1", 0, "incident response escalation policy"),
                        chunk("doc-1", 1, "password rotation every ninety days"),
                        chunk("doc-1", 2, "backup retention for audit logs")));

        List<Document> hits = provider.active()
                .similaritySearch(SearchRequest.builder()
                        .query("password rotation")
                        .topK(1)
                        .build());
        assertThat(hits).singleElement().satisfies(d -> assertThat(d.getText()).contains("password"));

        List<Chunk> neighbors = queries.byIndexRange(first, "doc-1", null, 0, 1);
        assertThat(neighbors).extracting(Chunk::chunkIndex).containsExactly(0, 1);
        assertThat(queries.countByDocId(first, "doc-1")).isEqualTo(3);

        VectorIndex second = provider.createIndex("mxbai-embed-large");
        assertThat(second.active()).isFalse();
        registry.activate(second.tableName());

        assertThat(provider.activeIndex().tableName()).isEqualTo("vector_store_mxbai_embed_large");
        assertThat(registry.all()).filteredOn(VectorIndex::active).hasSize(1);
    }

    @Test
    void refusesModelsThatAreNotConfigured() {
        assertThatThrownBy(() -> provider.createIndex("some-unknown-model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void tableNamesAreSanitised() {
        assertThat(VectorStoreProvider.tableNameFor("qwen3-embedding:0.6b"))
                .isEqualTo("vector_store_qwen3_embedding_0_6b");
    }

    private static Document chunk(String docId, int index, String text) {
        return Document.builder()
                .text(text)
                .metadata(
                        Map.of(MetadataKeys.DOC_ID, docId, MetadataKeys.CHUNK_INDEX, index, MetadataKeys.VERSION, "1"))
                .build();
    }
}
