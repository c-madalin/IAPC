package ro.iapc.rag.ingest;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import ro.iapc.rag.common.MetadataKeys;

/**
 * Turns one PDF into chunks with {@code page} and {@code chunk_index} set, numbered from 0 in reading order. Both
 * ingestion services use it, so local and remote PDFs are chunked the same way. Callers add the document-level keys
 * with {@link ChunkMetadata}.
 *
 * <p>Skeleton: page-based reading and token splitting without overlap. TODO(story-1-task-1): structure-aware chunking
 * with overlap, tables and multi-column layouts.
 */
public class PdfChunker {

    private final int chunkSize;

    /** @param chunkSize target chunk size in tokens */
    public PdfChunker(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive, got " + chunkSize);
        }
        this.chunkSize = chunkSize;
    }

    public List<Document> chunk(Resource pdf) {
        List<Document> pages = new PagePdfDocumentReader(pdf).read();
        List<Document> chunks =
                TokenTextSplitter.builder().withChunkSize(chunkSize).build().apply(pages);
        for (int i = 0; i < chunks.size(); i++) {
            var metadata = chunks.get(i).getMetadata();
            metadata.put(MetadataKeys.PAGE, metadata.getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, 0));
            metadata.put(MetadataKeys.CHUNK_INDEX, i);
        }
        return chunks;
    }
}
