package ro.iapc.rag.eval;

import java.util.List;

public record GoldenSet(String description, List<Question> questions) {

    /**
     * @param expectedDocIds documents that contain the answer; empty for unanswerable questions
     * @param answerable false means the system should refuse
     */
    public record Question(String id, String question, List<String> expectedDocIds, boolean answerable) {}
}
