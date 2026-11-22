# Service contracts

The Java records in `rag-common/src/main/java/ro/iapc/rag/common/api` are the source of truth; this page explains
them. Fields may be added freely. Renaming or removing one breaks other services: agree it with the team first.
Errors are RFC 9457 problem responses (`application/problem+json`) with `status` and `detail`.

## Shared types

`Chunk`: `id`, `docId`, `version`, `source`, `page`, `chunkIndex`, `text`, `score`. `score` is the vector
similarity (0 to 1, higher is closer) coming out of retrieval-pre and the reranker score after retrieval-post reranks.

`Stages`: `expand`, `rerank`, `stitch`, each a nullable boolean. `null` means "use the service default". The eval
sets them to measure the pipeline with and without each stage.

`Citation`: `docId`, `source`, `version`, `page`.

## rag-api

`POST /api/ask` (public, through the gateway). Body `{"query": "...", "stages": {...}}`. Every URL query parameter
is a metadata filter, forwarded untouched to retrieval-pre, for example `POST /api/ask?source=policy.pdf`.

Response: `answer`, `citations[]`, `refused`, `rankedChunks[]`, `topSimilarity`, `normalizedQuery`, `timingsMs`
(stage name to milliseconds). 400 for a blank query or an invalid filter, 502 when a stage fails.

## retrieval-pre (Story 3)

`POST /retrieve`. Body `{"query", "rawFilters": {"field": ["value", ...]}, "topN", "stages"}`. Returns
`{"chunks": [...], "normalizedQuery"}`, best first. 400 for a blank query or invalid filters. The 400 for invalid
filters must list the valid fields (Story 3 Task 1). The skeleton refuses every filter.

`GET /chunks/neighbors?docId=&version=&index=&window=1`. Chunks of the same document version with `chunkIndex`
in `[index - window, index + window]`, ordered by index. `version` is optional. `window` is 0 to 10.

## retrieval-post (Story 4)

`POST /answer`. Body `{"query", "chunks": [...], "stages"}`. Returns `{"answer", "citations", "refused",
"rankedChunks", "topSimilarity"}`. `rankedChunks` is what the answer was grounded on, in final order. The skeleton
refuses without calling the model when there are no chunks.

## ingest-static (Story 1)

- `POST /documents/sync`: ingests every PDF in `rag.ingest.resources`, returns the manifest entries written. A
  failing file is logged and skipped.
- `DELETE /documents/{docId}`: removes all chunks of the document, 404 if it has none.
- `GET /manifest`: every manifest row.

## ingest-dynamic (Story 2)

`POST /sources` and `POST /webhooks/vendor` return 501 until Story 2 implements them. URL fetching must not ship
before the SSRF guard (Task 4). The webhook must answer 401 on a bad HMAC signature (Task 3).

## Through the gateway

Each service is also reachable as `http://localhost:8080/<service-id>/<path>`, for example
`/retrieval-pre/retrieve` or `/ingest-static/manifest`. Story 3 Task 1's Postman collection should use these URLs.
