package ro.iapc.rag.ingest;

import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import ro.iapc.rag.common.MetadataKeys;

/**
 * The document-level metadata every chunk carries. {@link #applyTo} writes it onto each chunk. Web sources add
 * {@code source_url} and {@code fetched_at} themselves.
 *
 * @param docId stable id across versions: {@code static:<filename>}, {@code web:<normalized URL>} or {@code
 *     repo:<owner>/<repo>/<path>}
 * @param checksum SHA-256 of the source, see {@link Checksums}
 * @param embeddingModel model of the index the chunks are written to
 */
public record ChunkMetadata(String docId, String version, String source, String checksum, String embeddingModel) {

    public ChunkMetadata {
        Objects.requireNonNull(docId, "docId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(checksum, "checksum");
        Objects.requireNonNull(embeddingModel, "embeddingModel");
    }

    public List<Document> applyTo(List<Document> chunks) {
        for (Document chunk : chunks) {
            var metadata = chunk.getMetadata();
            metadata.put(MetadataKeys.DOC_ID, docId);
            metadata.put(MetadataKeys.VERSION, version);
            metadata.put(MetadataKeys.SOURCE, source);
            metadata.put(MetadataKeys.CHECKSUM, checksum);
            metadata.put(MetadataKeys.IS_LATEST, true);
            metadata.put(MetadataKeys.EMBEDDING_MODEL, embeddingModel);
        }
        return chunks;
    }
}
