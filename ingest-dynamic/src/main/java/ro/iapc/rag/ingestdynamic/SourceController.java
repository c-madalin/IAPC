package ro.iapc.rag.ingestdynamic;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Walking skeleton: endpoints exist so routing and contracts are in place, but nothing is fetched. URL fetching must
 * not ship before the SSRF guard (Story 2 Task 4), so these return 501 until then.
 */
@RestController
public class SourceController {

    /** TODO(story-2-task-1): ingest a web page, remote PDF or repository docs. */
    @PostMapping("/sources")
    public ProblemDetail ingestSource(@RequestBody(required = false) String body) {
        return notImplemented("URL ingestion arrives with Story 2 Tasks 1 and 4");
    }

    /** TODO(story-2-task-3): verify the HMAC signature (401 when invalid), dedupe, enqueue with retry. */
    @PostMapping("/webhooks/vendor")
    public ProblemDetail vendorWebhook(@RequestBody(required = false) String body) {
        return notImplemented("Webhook ingestion arrives with Story 2 Task 3");
    }

    private static ProblemDetail notImplemented(String detail) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, detail);
    }
}
