package ro.iapc.rag.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import ro.iapc.rag.common.ServiceNames;
import ro.iapc.rag.common.api.AnswerRequest;
import ro.iapc.rag.common.api.AnswerResponse;
import ro.iapc.rag.common.api.AskRequest;
import ro.iapc.rag.common.api.AskResponse;
import ro.iapc.rag.common.api.RetrieveRequest;
import ro.iapc.rag.common.api.RetrieveResponse;
import ro.iapc.rag.common.api.Stages;
import ro.iapc.rag.platform.ServiceClients;
import ro.iapc.rag.platform.StageTimer;

/** Runs one question through retrieval-pre, then retrieval-post. Holds no RAG logic of its own. */
@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final RestClient pre;
    private final RestClient post;
    private final StageTimer timer;

    public AskService(ServiceClients clients, StageTimer timer) {
        this.pre = clients.forService(ServiceNames.RETRIEVAL_PRE);
        this.post = clients.forService(ServiceNames.RETRIEVAL_POST);
        this.timer = timer;
    }

    public AskResponse ask(AskRequest request, Map<String, List<String>> filters) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        Stages stages = Stages.orDefaults(request.stages());
        Map<String, Long> timings = new LinkedHashMap<>();

        RetrieveResponse retrieved = timer.time(
                "pre",
                timings,
                () -> call(
                        ServiceNames.RETRIEVAL_PRE,
                        pre,
                        "/retrieve",
                        new RetrieveRequest(request.query(), filters, null, stages),
                        RetrieveResponse.class));

        AnswerResponse answer = timer.time(
                "post",
                timings,
                () -> call(
                        ServiceNames.RETRIEVAL_POST,
                        post,
                        "/answer",
                        new AnswerRequest(request.query(), retrieved.chunks(), stages),
                        AnswerResponse.class));

        log.info(
                "Answered with {} candidates, refused={}, timings={}",
                retrieved.chunks().size(),
                answer.refused(),
                timings);
        return new AskResponse(
                answer.answer(),
                answer.citations(),
                answer.refused(),
                answer.rankedChunks(),
                answer.topSimilarity(),
                retrieved.normalizedQuery(),
                timings);
    }

    /** Passes 4xx from a stage through unchanged (the client sent something wrong); anything else becomes a 502. */
    private static <T> T call(String service, RestClient client, String path, Object body, Class<T> type) {
        try {
            T result = client.post().uri(path).body(body).retrieve().body(type);
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, service + " returned an empty body");
            }
            return result;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(e.getStatusCode(), service + ": " + e.getResponseBodyAsString(), e);
            }
            log.error("{} failed with {}", service, e.getStatusCode(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, service + " failed: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.error("{} unreachable", service, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, service + " unreachable", e);
        }
    }
}
