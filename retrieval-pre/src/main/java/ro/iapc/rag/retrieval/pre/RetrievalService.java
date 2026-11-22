package ro.iapc.rag.retrieval.pre;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import ro.iapc.rag.common.api.Chunk;
import ro.iapc.rag.common.api.RetrieveRequest;
import ro.iapc.rag.common.api.RetrieveResponse;
import ro.iapc.rag.platform.StageTimer;
import ro.iapc.rag.store.ChunkMapper;
import ro.iapc.rag.store.ChunkQueries;
import ro.iapc.rag.store.VectorIndex;
import ro.iapc.rag.store.VectorStoreProvider;

/**
 * Walking-skeleton retrieval: plain similarity search on the active index. Story 3 adds, in this order: filter
 * validation (Task 1), query normalization (Task 2), acronym expansion (Task 3) and multi-query fusion (Task 4).
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final VectorStoreProvider stores;
    private final ChunkQueries chunkQueries;
    private final RetrievalProperties properties;
    private final StageTimer timer;

    public RetrievalService(
            VectorStoreProvider stores, ChunkQueries chunkQueries, RetrievalProperties properties, StageTimer timer) {
        this.stores = stores;
        this.chunkQueries = chunkQueries;
        this.properties = properties;
        this.timer = timer;
    }

    public RetrieveResponse retrieve(RetrieveRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (request.rawFilters() != null && !request.rawFilters().isEmpty()) {
            // TODO(story-3-task-1): parse into a validated filter object instead of refusing.
            throw new IllegalArgumentException("Metadata filters are not supported yet. Got: "
                    + request.rawFilters().keySet());
        }
        int topN = Math.min(request.topN() == null ? properties.defaultTopN() : request.topN(), properties.maxTopN());
        String normalized = request.query().strip();

        List<Chunk> chunks = timer.time(
                "vector_search",
                () -> stores
                        .active()
                        .similaritySearch(SearchRequest.builder()
                                .query(normalized)
                                .topK(topN)
                                .similarityThresholdAll()
                                .build())
                        .stream()
                        .map(ChunkMapper::toChunk)
                        .toList());
        log.info("Retrieved {} chunks for query of {} chars (topN={})", chunks.size(), normalized.length(), topN);
        return new RetrieveResponse(chunks, normalized);
    }

    /** Chunks around {@code index} in the same document version, for neighbor stitching in retrieval-post. */
    public List<Chunk> neighbors(String docId, String version, int index, int window) {
        if (window < 0 || window > 10) {
            throw new IllegalArgumentException("window must be between 0 and 10");
        }
        VectorIndex active = stores.activeIndex();
        return chunkQueries.byIndexRange(active, docId, version, Math.max(0, index - window), index + window);
    }
}
