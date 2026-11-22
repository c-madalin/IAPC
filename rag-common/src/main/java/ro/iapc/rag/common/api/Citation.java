package ro.iapc.rag.common.api;

public record Citation(String docId, String source, String version, Integer page) {

    public static Citation of(Chunk chunk) {
        return new Citation(chunk.docId(), chunk.source(), chunk.version(), chunk.page());
    }
}
