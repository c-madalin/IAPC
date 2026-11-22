Dear students,

I would like one member of each team to act as project manager and to discuss with me, in my role as product owner, the ideas that emerge from your team’s brainstorming, so that together we can identify the best solutions.

For the purposes of this exercise, I will act as both product owner and project manager for a generic team. This team has four members, although teams in this project may have between one and four.

Our goal is to develop an advanced system that improves the quality of an intelligent application while reducing its operating costs.

As project manager, I have defined two epics: **RAG** and **MCP**. The deadline for the first epic is **December 7th**.

**Story 1: Static ingestion resources**

* Task 1: PDF ingestion: parse, structure-aware chunking with overlap, embed, and store with the metadata schema and stable `doc_id`  
* Task 2: Update process: checksum comparison, idempotent re-ingest, atomic replacement of old chunks  
* Task 3: Delete process: remove all chunks by `doc_id`, with a check for orphaned vectors  
* Task 4: Ingestion manifest and re-index support: log of what was ingested, when, and with which embedding model version

**Story 2: Dynamic ingestion resources**

* Task 1: Internet ingestion (pages, files, third-party repos) with boilerplate cleanup  
* Task 2: Versioning: old versions stay in the store with `version`/`is_latest` metadata, and retrieval defaults to latest  
* Task 3: Automation: file watcher and vendor webhook receiver with signature verification and a retry queue  
* Task 4: Security: SSRF protection, domain allowlist, size and content-type limits, and sanitization of ingested content

**Story 3: Pre-retrieval pipeline**

* Task 1: Safe metadata filtering: URL parameters parsed into a validated filter object, whitelisted fields, explicit error on invalid filters  
* Task 2: Query normalization: precompiled, ordered, ReDoS-safe regex rules, polite-prefix stripping, question-mark trimming  
* Task 3: Acronym and synonym expansion that appends the full form, with passthrough on zero matches  
* Task 4: Query expander (multi-query) with result fusion, and a limit on added latency and cost

**Story 4: Post-retrieval and grounding**

* Task 1: Reranker (cross-encoder): retrieve a wider top-k, rerank down to a few  
* Task 2: Neighbor stitching using `chunk_index`, with overlap deduplication and a token budget  
* Task 3: Citation headers (source, version, page) and a grounded prompt with the similarity-threshold “I don’t know” fallback  
* Task 4: Evaluation harness: golden Q\&A set, recall@k, MRR, faithfulness check, and a regression run before each merge

Each task is done only when its own criteria and the baseline below are both met. Numeric targets (for example 30 seconds or 85%) are proposals to agree with the product owner.

### Baseline for every task

* Code is peer-reviewed and merged to the main branch.  
* Automated tests pass, with at least one test for the success path and one for a failure path.  
* No secrets in the repository, and configuration is documented.  
* Errors and key events are logged.  
* The README or docs are updated, and the result is demonstrated to the product owner.

### Epic 1: RAG (deadline December 7\)

#### Story 1: Static ingestion resources

**Task 1, PDF ingestion**

* A sample set of PDFs (including one with tables and one with multiple columns) is ingested end to end.  
* Every chunk carries the full metadata schema (`doc_id`, `version`, `source`, `page`, `chunk_index`, `checksum`).  
* Chunk size, overlap and embedding model are configurable and documented.  
* Re-running the same file creates no duplicates.  
* A manual check of 20 random chunks shows no broken or garbled text.

**Task 2, Update process**

* A changed file is detected by checksum, and an unchanged file is skipped.  
* Old chunks are replaced atomically, so queries never see a mix of old and new content or an empty result.  
* A failure mid-update leaves the previous version intact.  
* A test confirms the chunk count after the update matches the expected count.

**Task 3, Delete process**

* Deleting by `doc_id` removes all of its chunks.  
* A query about the deleted content no longer returns it.  
* An orphan check reports zero leftover vectors.  
* Deleting a non-existent document is safe and returns a clear message.

**Task 4, Manifest and re-index**

* The manifest records `doc_id`, checksum, version, timestamp, embedding model and chunk count.  
* A reconciliation script shows zero drift between manifest and vector database.  
* A full re-index with a different embedding model works on the sample set.

#### Story 2: Dynamic ingestion resources

**Task 1, Internet ingestion**

* A web page, a remote PDF and one third-party repository’s docs are ingested successfully.  
* Boilerplate (navigation, footers, cookie banners) is removed.  
* Source URL and fetch date are stored in the metadata.  
* One failed source is logged and does not stop the rest of the batch.

**Task 2, Versioning**

* Ingesting a new version keeps the old chunks, marked `is_latest=false` with `valid_from`.  
* A default query returns only the latest version.  
* A query with an explicit version parameter returns the older content.  
* A test with two versions of one document proves both behaviours.

**Task 3, Automation**

* The watcher detects added, changed and deleted files within 30 seconds and triggers ingestion.  
* The webhook endpoint verifies the HMAC signature and rejects invalid requests with 401\.  
* A valid vendor notification triggers ingestion with no manual step.  
* Duplicate events are deduplicated, and failed jobs retry with backoff and are visible in logs.

**Task 4, Security**

* Requests to localhost, private IP ranges and cloud metadata addresses are blocked.  
* A domain allowlist, HTTPS-only rule, timeout, size limit and content-type check are enforced.  
* Scripts and active content are stripped from ingested pages.  
* A documented set of negative tests proves each control.

#### Story 3: Pre-retrieval pipeline

**Task 1, Safe metadata filtering**

* URL parameters are parsed into a validated filter object, and raw strings never reach the vector database.  
* Only whitelisted metadata fields and operators are accepted.  
* An invalid filter returns a 400 error listing the valid fields.  
* Multi-variable filters (AND/OR) work, and a committed Postman collection demonstrates them.  
* A null filter falls through to the standard flow.

**Task 2, Query normalization**

* Rules are ordered, precompiled and idempotent.  
* A ReDoS test with adversarial input finishes under a set time limit.  
* Polite prefixes and trailing question marks are stripped, covered by a table of at least 15 test cases.  
* The original query is preserved in the logs.

**Task 3, Acronym expansion**

* The acronym dictionary is an external, editable file.  
* The search uses both the acronym and the full form.  
* Matching respects word boundaries, so partial matches inside other words don’t trigger.  
* Zero matches pass the original query through with no exception.

**Task 4, Multi-query expander**

* The expander generates a configurable number of variants (for example 3), and results are fused (for example with reciprocal rank fusion).  
* Added latency and token cost are measured and logged, with a configured cap.  
* If the expander fails or times out, the pipeline falls back to the original query.  
* Recall@k with and without the expander is reported against the baseline.

#### Story 4: Post-retrieval and grounding

**Task 1, Reranker**

* The pipeline retrieves a wider top-N (for example 20\) and reranks down to top-k (for example 5).  
* The golden set shows measured improvement in MRR or recall@k compared to no reranking.  
* The model version is pinned, and added latency is documented.  
* If the reranker fails, the pipeline keeps the original order.

**Task 2, Neighbor stitching**

* Adjacent chunks are merged by `chunk_index`, with overlapping text deduplicated.  
* A token budget is enforced, and stitching never crosses document boundaries.  
* Tests cover the first chunk, last chunk and a single-chunk document.

**Task 3, Citations and grounding**

* Every context block has a header with source, version and page.  
* The prompt instructs the model to answer only from the provided context and to cite sources.  
* Below the similarity threshold, the system answers “I don’t know.”  
* The threshold is justified with eval data, and 10 out-of-scope questions are all refused correctly.

**Task 4, Evaluation harness**

* The golden set has at least 30 question-and-source pairs, including unanswerable questions.  
* It reports recall@k, MRR and a faithfulness check.  
* One command runs it, and a baseline report is committed.  
* A regression run is required before merging pipeline changes.

![][image1]

**To run it**

1. Copy `.env.example` to `.env`, then set `POSTGRES_PASSWORD`, `LLM_API_KEY` and `VENDOR_WEBHOOK_SECRET`. Compose refuses to start if any of the three is empty.  
2. Put the PDFs in `./resources`. It’s mounted read-only into both ingestion services.  
3. Run `docker compose up --build -d`.

**Startup order**

1. `pgvector` and `eureka-server` start first and must report healthy.  
2. `ingest-static` follows, and it is the only service that creates the `vector_store` table. The other three wait for it, which avoids four services racing to create the same table.  
3. The gateway starts after Eureka.

**Exposed ports**

* Only the gateway (8080) is public.  
* The Eureka dashboard (8761) and Postgres (5432) are bound to localhost.  
* The services themselves are reachable only through the gateway.

**What to adjust**

* **Module names:** They must match the folders `eureka-server`, `api-gateway`, `ingest-static`, `ingest-dynamic`, `retrieval-pre` and `retrieval-post`. All of them also need `spring-boot-starter-actuator` for the healthchecks.  
* **Embedding size:** `EMBEDDING_DIMENSIONS` must match your embedding model (1536 for `text-embedding-3-small`). Changing the model later means re-indexing.  
* **Gateway property prefix:** The gateway uses Eureka-based routing (`/ingest-static/**`, `/retrieval-pre/**`, and so on). On Spring Cloud 2025.0 and later the property prefix is `spring.cloud.gateway.server.webflux.*`, so adjust it if you’re on that version.  
* **`APP_*` variables:** Names like `APP_CHUNK_SIZE` and `APP_RERANK_TOP_K` are placeholders of mine. Map them to the property names in your own code.  
* **SSRF protection:** `APP_INGEST_BLOCK_PRIVATE_NETWORKS` only signals the setting. The actual block on internal IP ranges must be implemented in `ingest-dynamic`, since Compose can’t enforce it.

**Versions chosen**

* **Spring Boot 4.0.7 with Spring Cloud 2025.1.2:** The release notes list these as compatible.  
* **Spring AI 2.0.0:** It requires Boot 4\. Boot 3.5 reached end of life on June 30, 2026, so I did not use the 1.x line.  
* **Java 21:** This matches the Dockerfile.

Bump these versions in the parent `pom.xml` if you want newer patch releases.

**How the config is wired**

* **Gateway:** It uses `spring-cloud-starter-gateway-server-webflux` and builds routes from Eureka (`/ingest-static/**`, `/retrieval-pre/**` and so on). Every registered service is exposed that way, which is fine for coursework but needs explicit routes in production.  
* **Schema creation:** Only `ingest-static` has `initialize-schema: true`, which matches the startup order in Compose.  
* **`app.*` properties:** Each one is an explicit `${APP_...}` placeholder, so the Compose variables bind without relying on relaxed-binding rules. These properties are my placeholders, so map them to the classes you write.  
* **Local runs:** Without Compose, the services default to `localhost` for the database and Eureka.

**Things to verify on first build**

* **Starter and property names:** I could not check the Spring AI 2.0 and Boot 4 names against a real build. These are `spring-boot-starter-webmvc`, `spring-ai-starter-model-openai`, `spring-ai-starter-vector-store-pgvector` and the two document readers. Spring AI 2.0 also changed its provider SDKs and options, so the `spring.ai.openai.*` properties may differ slightly.  
* **Missing files:** `retrieval-pre` points to `classpath:acronyms.yml` and `retrieval-post` to `classpath:eval/golden-set.json`. Neither file exists yet.