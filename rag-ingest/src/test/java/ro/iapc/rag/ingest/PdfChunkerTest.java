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
}
