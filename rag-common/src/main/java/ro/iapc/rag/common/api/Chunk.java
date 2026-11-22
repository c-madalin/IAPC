package ro.iapc.rag.common.api;

/**
 * One retrieved chunk as it travels between services.
 *
 * @param id vector store row id
 * @param score similarity (0..1, higher is closer) or reranker score, depending on the stage that produced it
 */
public record Chunk(
        String id,
        String docId,
        String version,
        String source,
        Integer page,
        Integer chunkIndex,
        String text,
        Double score) {

    public Chunk withScore(Double newScore) {
        return new Chunk(id, docId, version, source, page, chunkIndex, text, newScore);
    }

    public Chunk withText(String newText) {
        return new Chunk(id, docId, version, source, page, chunkIndex, newText, score);
    }
}
