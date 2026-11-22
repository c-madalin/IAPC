package ro.iapc.rag.retrieval.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ro.iapc.rag.common.api.AnswerRequest;
import ro.iapc.rag.common.api.AnswerResponse;

@RestController
public class AnswerController {

    private final AnswerService answers;

    public AnswerController(AnswerService answers) {
        this.answers = answers;
    }

    @PostMapping("/answer")
    public AnswerResponse answer(@RequestBody AnswerRequest request) {
        return answers.answer(request);
    }
}
