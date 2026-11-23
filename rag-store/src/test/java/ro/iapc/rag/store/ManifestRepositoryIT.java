package ro.iapc.rag.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import ro.iapc.rag.store.ManifestEntry.SourceType;
import ro.iapc.rag.testsupport.PgvectorTestcontainer;

@SpringBootTest(classes = TestStoreApplication.class)
@Import(PgvectorTestcontainer.class)
@DirtiesContext
class ManifestRepositoryIT {

    @Autowired
    ManifestRepository manifest;

    @Autowired
    VectorStoreProvider stores;

    @Test
    void storesBothSourceTypesAndUpsertReplacesTheRow() {
        String table = stores.ensureActiveIndex().tableName();

        manifest.upsert(entry("static:a.pdf", SourceType.STATIC, table, 3));
        manifest.upsert(entry("web:https://example.com/a", SourceType.DYNAMIC, table, 5));
        manifest.upsert(entry("static:a.pdf", SourceType.STATIC, table, 4));

        assertThat(manifest.findAll())
                .extracting(ManifestEntry::docId, ManifestEntry::sourceType, ManifestEntry::chunkCount)
                .containsExactly(
                        tuple("static:a.pdf", SourceType.STATIC, 4),
                        tuple("web:https://example.com/a", SourceType.DYNAMIC, 5));
        assertThat(manifest.findAll())
                .allSatisfy(e -> assertThat(e.ingestedAt()).isNotNull());

        assertThat(manifest.deleteByDocId("static:a.pdf", table)).isEqualTo(1);
        assertThat(manifest.deleteByDocId("static:a.pdf", table)).isZero();
    }

    @Test
    void refusesARowForAnUnknownVectorTable() {
        assertThatThrownBy(() -> manifest.upsert(entry("static:b.pdf", SourceType.STATIC, "vector_store_missing", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsTheLatestManifestRowForAnIndex() {
        String table = stores.ensureActiveIndex().tableName();

        manifest.upsert(entry("static:a.pdf", SourceType.STATIC, table, 3));

        assertThat(manifest.findLatest("static:a.pdf", table))
                .get()
                .extracting(ManifestEntry::checksum, ManifestEntry::chunkCount)
                .containsExactly("abc", 3);
        assertThat(manifest.findLatest("static:missing.pdf", table)).isEmpty();
    }

    private static ManifestEntry entry(String docId, SourceType type, String table, int chunks) {
        return new ManifestEntry(docId, "1", "a.pdf", type, "abc", "nomic-embed-text", table, chunks, null);
    }
}
