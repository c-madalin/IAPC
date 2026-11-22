package ro.iapc.rag.api;

import java.util.List;
import java.util.Map;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.iapc.rag.common.api.AskRequest;
import ro.iapc.rag.common.api.AskResponse;

@RestController
@RequestMapping("/api")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    /**
     * Every URL query parameter is treated as a metadata filter and forwarded untouched to retrieval-pre, which
     * validates it (Story 3 Task 1). Example: {@code POST /api/ask?source=policy.pdf}.
     */
    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request, @RequestParam MultiValueMap<String, String> params) {
        Map<String, List<String>> filters = Map.copyOf(params);
        return askService.ask(request, filters);
    }
}
