package ro.iapc.rag.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import ro.iapc.rag.common.MetadataKeys;

class ChunkMetadataTest {

    @Test
    void writesEveryDocumentLevelKeyAndKeepsChunkKeys() {
        Document chunk = Document.builder()
                .text("text")
                .metadata(MetadataKeys.CHUNK_INDEX, 3)
                .build();

        new ChunkMetadata("static:a.pdf", "1", "a.pdf", "abc", "nomic-embed-text").applyTo(List.of(chunk));

        assertThat(chunk.getMetadata())
                .containsEntry(MetadataKeys.DOC_ID, "static:a.pdf")
                .containsEntry(MetadataKeys.VERSION, "1")
                .containsEntry(MetadataKeys.SOURCE, "a.pdf")
                .containsEntry(MetadataKeys.CHECKSUM, "abc")
                .containsEntry(MetadataKeys.IS_LATEST, true)
                .containsEntry(MetadataKeys.EMBEDDING_MODEL, "nomic-embed-text")
                .containsEntry(MetadataKeys.CHUNK_INDEX, 3);
    }

    @Test
    void refusesAMissingDocId() {
        assertThatThrownBy(() -> new ChunkMetadata(null, "1", "a.pdf", "abc", "m"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("docId");
    }
}
