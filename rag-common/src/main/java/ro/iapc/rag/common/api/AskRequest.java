package ro.iapc.rag.common.api;

/** Body of {@code POST /api/ask} on rag-api. Metadata filters arrive as URL query parameters, not in the body. */
public record AskRequest(String query, Stages stages) {}
