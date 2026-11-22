package ro.iapc.rag.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem responses and logs them once. Spring MVC's own exceptions (bad JSON, 404,
 * validation) keep their status through {@link ResponseEntityExceptionHandler}. Throw {@link ResponseStatusException}
 * for expected client errors; anything else is a 500 and gets logged with its stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleStatus(ResponseStatusException e) {
        log.warn("Request failed with {}: {}", e.getStatusCode(), e.getReason());
        ProblemDetail problem = ProblemDetail.forStatus(e.getStatusCode());
        problem.setDetail(e.getReason());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadInput(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error, see service logs");
    }
}
