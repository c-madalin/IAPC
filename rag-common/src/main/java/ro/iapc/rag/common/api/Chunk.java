package ro.iapc.rag.common.api;

/**
 * One retrieved chunk as it travels between services.
 *
 * @param id vector store row id
 * @param score ranking score of the last stage that ordered the chunks: vector similarity from retrieval-pre, the
 *     fused score after multi-query, the reranker score after reranking. Only compare it within one stage
 * @param similarity cosine similarity to the query (0..1, higher is closer), set by the vector search and never
 *     overwritten by later stages. The "I don't know" threshold compares against this. With multi-query, the highest
 *     similarity over all query variants
 */
public record Chunk(
        String id,
        String docId,
        String version,
        String source,
        Integer page,
        Integer chunkIndex,
        String text,
        Double score,
        Double similarity) {

    public Chunk withScore(Double newScore) {
        return new Chunk(id, docId, version, source, page, chunkIndex, text, newScore, similarity);
    }

    public Chunk withText(String newText) {
        return new Chunk(id, docId, version, source, page, chunkIndex, newText, score, similarity);
    }
}
