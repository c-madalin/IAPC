package ro.iapc.rag.common.api;

import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /retrieve} on retrieval-pre.
 *
 * @param rawFilters URL query parameters exactly as the client sent them. retrieval-pre validates them; nobody else
 *     interprets them, and they never reach the database as strings.
 * @param topN how many candidates to return; {@code null} means the service default
 */
public record RetrieveRequest(String query, Map<String, List<String>> rawFilters, Integer topN, Stages stages) {}
