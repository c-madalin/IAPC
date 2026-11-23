package ro.iapc.rag.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import ro.iapc.rag.common.MetadataKeys;

class PdfChunkerTest {

    @Test
    void numbersChunksInReadingOrderWithTheirPage() {
        List<Document> chunks = new PdfChunker(400).chunk(new ClassPathResource("pdfs/scope_roles.pdf"));

        assertThat(chunks).isNotEmpty();
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getMetadata())
                    .containsEntry(MetadataKeys.CHUNK_INDEX, i)
                    .containsKey(MetadataKeys.PAGE);
            assertThat(chunks.get(i).getText()).isNotBlank();
        }
    }

    @Test
    void smallerChunkSizeGivesMoreChunks() {
        var pdf = new ClassPathResource("pdfs/scope_roles.pdf");

        assertThat(new PdfChunker(100).chunk(pdf))
                .hasSizeGreaterThan(new PdfChunker(800).chunk(pdf).size());
    }

    @Test
    void failsOnAFileThatIsNotAPdf() {
        var notPdf = new ByteArrayResource("plain text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new PdfChunker(400).chunk(notPdf)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsANonPositiveChunkSize() {
        assertThatThrownBy(() -> new PdfChunker(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repeatsConfiguredOverlapAtAdjacentChunkBoundary() {
        List<String> chunks = PdfChunker.split("one two three four five six seven eight nine ten", 4, 2);

        assertThat(chunks)
                .containsExactly(
                        "one two three four", "three four five six", "five six seven eight", "seven eight nine ten");
    }

    @Test
    void rejectsOverlapAtOrAboveChunkSize() {
        assertThatThrownBy(() -> new PdfChunker(4, 4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsTableFixture() {
        List<Document> chunks = new PdfChunker(400, 50).chunk(new ClassPathResource("pdfs/table.pdf"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream()
                        .map(Document::getText)
                        .anyMatch(text -> text.contains("Response time") && text.contains("Security team")))
                .isTrue();
    }

    @Test
    void readsMultiColumnFixture() {
        List<Document> chunks = new PdfChunker(400, 50).chunk(new ClassPathResource("pdfs/multicolumn.pdf"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream()
                        .map(Document::getText)
                        .anyMatch(text -> text.contains("twelve characters") && text.contains("fifteen minutes")))
                .isTrue();
    }
}
