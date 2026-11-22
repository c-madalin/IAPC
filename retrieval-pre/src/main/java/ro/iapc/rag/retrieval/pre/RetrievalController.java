package ro.iapc.rag.retrieval.pre;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.iapc.rag.common.api.NeighborsResponse;
import ro.iapc.rag.common.api.RetrieveRequest;
import ro.iapc.rag.common.api.RetrieveResponse;

@RestController
public class RetrievalController {

    private final RetrievalService retrieval;

    public RetrievalController(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @PostMapping("/retrieve")
    public RetrieveResponse retrieve(@RequestBody RetrieveRequest request) {
        return retrieval.retrieve(request);
    }

    @GetMapping("/chunks/neighbors")
    public NeighborsResponse neighbors(
            @RequestParam String docId,
            @RequestParam(required = false) String version,
            @RequestParam int index,
            @RequestParam(defaultValue = "1") int window) {
        return new NeighborsResponse(retrieval.neighbors(docId, version, index, window));
    }
}
