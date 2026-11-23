package ro.iapc.rag.ingest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import ro.iapc.rag.common.MetadataKeys;

/**
 * Turns one PDF into chunks with {@code page} and {@code chunk_index} set, numbered from 0 in reading order. Both
 * ingestion services use it, so local and remote PDFs are chunked the same way. Callers add the document-level keys
 * with {@link ChunkMetadata}.
 *
 * <p>Text is split within each page into overlapping word windows.
 */
public class PdfChunker {

    private final int chunkSize;
    private final int chunkOverlap;

    /** @param chunkSize target chunk size in words */
    public PdfChunker(int chunkSize) {
        this(chunkSize, 0);
    }

    public PdfChunker(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive, got " + chunkSize);
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap must not be negative, got " + chunkOverlap);
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be smaller than chunkSize");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<Document> chunk(Resource pdf) {
        List<Document> pages = new PagePdfDocumentReader(pdf).read();
        List<Document> chunks = new ArrayList<>();
        for (Document page : pages) {
            String text = page.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            Object pageNumber = page.getMetadata().getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, 0);
            for (String chunkText : split(text, chunkSize, chunkOverlap)) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put(MetadataKeys.PAGE, pageNumber);
                chunks.add(Document.builder().text(chunkText).metadata(metadata).build());
            }
        }
        for (int i = 0; i < chunks.size(); i++) {
            var metadata = chunks.get(i).getMetadata();
            metadata.put(MetadataKeys.CHUNK_INDEX, i);
        }
        return chunks;
    }

    static List<String> split(String text, int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0 || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunk size and overlap are invalid");
        }
        String normalized = text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> words = Arrays.asList(normalized.split(" "));
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - chunkOverlap;
        for (int start = 0; start < words.size(); start += step) {
            int end = Math.min(start + chunkSize, words.size());
            chunks.add(String.join(" ", words.subList(start, end)));
            if (end == words.size()) {
                break;
            }
        }
        return chunks;
    }
}
