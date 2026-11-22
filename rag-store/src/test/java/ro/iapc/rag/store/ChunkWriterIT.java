package ro.iapc.rag.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import ro.iapc.rag.common.MetadataKeys;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.store.ChunkWriter.Retire;
import ro.iapc.rag.testsupport.FakeEmbeddingModel;
import ro.iapc.rag.testsupport.PgvectorTestcontainer;

@SpringBootTest(classes = TestStoreApplication.class)
@Import({PgvectorTestcontainer.class, ChunkWriterIT.FailingEmbeddings.class})
@DirtiesContext
class ChunkWriterIT {

    static final AtomicBoolean EMBEDDING_FAILS = new AtomicBoolean();

    @TestConfiguration
    static class FailingEmbeddings {
        @Bean
        @Primary
        EmbeddingModelFactory switchableEmbeddings(RagStoreProperties properties) {
            return model -> new FakeEmbeddingModel(properties.dimensionsOf(model)) {
                @Override
                public EmbeddingResponse call(EmbeddingRequest request) {
                    if (EMBEDDING_FAILS.get()) {
                        throw new IllegalStateException("ollama down");
                    }
                    return super.call(request);
                }
            };
        }
    }

    @Autowired
    ChunkWriter writer;

    @Autowired
    VectorStoreProvider stores;

    @Autowired
    ChunkQueries queries;

    @Autowired
    JdbcTemplate jdbc;

    VectorIndex index;

    @BeforeEach
    void cleanIndex() {
        EMBEDDING_FAILS.set(false);
        index = stores.ensureActiveIndex();
        jdbc.update("delete from " + index.tableName());
    }

    @Test
    void deleteRetireReplacesTheDocumentWithTheNewChunks() {
        writer.write(index, "static:a.pdf", chunks("static:a.pdf", "1", "old password text", 3), Retire.DELETE);

        var result =
                writer.write(index, "static:a.pdf", chunks("static:a.pdf", "1", "new password text", 2), Retire.DELETE);

        assertThat(result.published()).isEqualTo(2);
        assertThat(result.retired()).isEqualTo(3);
        assertThat(queries.countByDocId(index, "static:a.pdf")).isEqualTo(2);
        assertThat(latest("password"))
                .extracting(Document::getText)
                .allSatisfy(text -> assertThat(text).startsWith("new"));
    }

    @Test
    void flagRetireKeepsTheOldVersionOutOfDefaultSearches() {
        writer.write(
                index,
                "web:https://x.test/a",
                chunks("web:https://x.test/a", "1", "old password text", 2),
                Retire.FLAG);

        var result = writer.write(
                index,
                "web:https://x.test/a",
                chunks("web:https://x.test/a", "2", "new password text", 2),
                Retire.FLAG);

        assertThat(result.retired()).isEqualTo(2);
        assertThat(latest("password"))
                .extracting(d -> d.getMetadata().get(MetadataKeys.VERSION))
                .containsOnly("2");
        assertThat(queries.byIndexRange(index, "web:https://x.test/a", "1", 0, 10))
                .extracting(Chunk::text)
                .allSatisfy(text -> assertThat(text).startsWith("old"))
                .hasSize(2);
    }

    @Test
    void stagedChunksAreInvisibleUntilPublished() {
        writer.write(index, "static:a.pdf", chunks("static:a.pdf", "1", "old password text", 2), Retire.DELETE);
        List<Document> next = chunks("static:a.pdf", "1", "new password text", 2);

        String version = writer.stage(index, "static:a.pdf", next);

        assertThat(latest("password"))
                .extracting(Document::getText)
                .allSatisfy(text -> assertThat(text).startsWith("old"));
        assertThat(queries.byIndexRange(index, "static:a.pdf", null, 0, 10)).hasSize(2);
        assertThat(searchVersion("password", "1")).hasSize(2);

        writer.publish(
                index,
                "static:a.pdf",
                version,
                next.stream().map(Document::getId).toList(),
                Retire.DELETE);

        assertThat(latest("password"))
                .extracting(Document::getText)
                .allSatisfy(text -> assertThat(text).startsWith("new"));
    }

    @Test
    void failedEmbeddingLeavesThePreviousVersionAndNoStagedRows() {
        writer.write(index, "static:a.pdf", chunks("static:a.pdf", "1", "old password text", 2), Retire.DELETE);
        EMBEDDING_FAILS.set(true);

        assertThatThrownBy(() -> writer.write(
                        index, "static:a.pdf", chunks("static:a.pdf", "1", "new password text", 3), Retire.DELETE))
                .hasMessageContaining("ollama down");

        EMBEDDING_FAILS.set(false);
        assertThat(queries.countByDocId(index, "static:a.pdf")).isEqualTo(2);
        assertThat(latest("password"))
                .extracting(Document::getText)
                .allSatisfy(text -> assertThat(text).startsWith("old"));
    }

    @Test
    void publishThatFindsTooFewStagedChunksRollsBackTheRetire() {
        writer.write(index, "static:a.pdf", chunks("static:a.pdf", "1", "old password text", 2), Retire.DELETE);

        assertThatThrownBy(() -> writer.publish(
                        index, "static:a.pdf", "1", List.of("00000000-0000-0000-0000-000000000001"), Retire.DELETE))
                .isInstanceOf(IllegalStateException.class);

        assertThat(queries.countByDocId(index, "static:a.pdf")).isEqualTo(2);
    }

    @Test
    void refusesChunksOfAnotherDocumentOrWithoutVersion() {
        assertThatThrownBy(
                        () -> writer.write(index, "static:a.pdf", chunks("static:b.pdf", "1", "x", 1), Retire.DELETE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> writer.write(index, "static:a.pdf", List.of(), Retire.DELETE))
                .isInstanceOf(IllegalArgumentException.class);
        Document noVersion = Document.builder()
                .text("x")
                .metadata(MetadataKeys.DOC_ID, "static:a.pdf")
                .build();
        assertThatThrownBy(() -> writer.write(index, "static:a.pdf", List.of(noVersion), Retire.DELETE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(queries.countByDocId(index, "static:a.pdf")).isZero();
    }

    private List<Document> latest(String query) {
        return stores.forIndex(index)
                .similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(20)
                        .similarityThresholdAll()
                        .filterExpression(new FilterExpressionBuilder()
                                .eq(MetadataKeys.IS_LATEST, true)
                                .build())
                        .build());
    }

    /** What an explicit version query sees. Before publish, only the old chunks match. */
    private List<Document> searchVersion(String query, String version) {
        return stores.forIndex(index)
                .similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(20)
                        .similarityThresholdAll()
                        .filterExpression(new FilterExpressionBuilder()
                                .eq(MetadataKeys.VERSION, version)
                                .build())
                        .build());
    }

    private static List<Document> chunks(String docId, String version, String text, int count) {
        List<Document> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(Document.builder()
                    .text(text + " " + i)
                    .metadata(new HashMap<>(Map.of(
                            MetadataKeys.DOC_ID, docId,
                            MetadataKeys.VERSION, version,
                            MetadataKeys.CHUNK_INDEX, i,
                            MetadataKeys.IS_LATEST, true)))
                    .build());
        }
        return chunks;
    }
}
