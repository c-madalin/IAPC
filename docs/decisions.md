# Decisions

Agreed on 2026-11-22 when the base setup was planned. Add a dated entry when a decision changes; don't rewrite
history.

| Topic | Decision | Why |
|---|---|---|
| Stack | Spring Boot 4.0.10, Spring Cloud 2025.1.5, Spring AI 2.0.3, Java 21, Maven wrapper | The professor's lines (Boot 4.0, Cloud 2025.1, AI 2.0) with the latest patches. Spring AI 2.0.3 and Cloud 2025.1.5 are both built against Boot 4.0.10. Bump all three together |
| Models | Ollama: `qwen2.5:7b` for chat, `nomic-embed-text` (768 dims) for embeddings | No API costs or keys. 7B follows grounding and citation instructions much better than 1.5B and fits 8 GB of VRAM or an M-series Mac |
| Ollama location | On the host by default; optional `ollama-gpu` compose profile for NVIDIA | Docker on macOS can't use the Apple GPU |
| Vector DB | pgvector | Matches the professor's design. Qdrant from the class demo is gone |
| Old demo code | Removed (still in git history). The six PDFs moved to `resources/` | Fresh multi-module layout |
| Team split | 3 people, all four stories built, one person takes two; infra owned by the two infra owners | |
| Request flow | Separate `rag-api` orchestrator calls retrieval-pre, then retrieval-post | Each story is a stage that can be tested alone; the eval hits one endpoint |
| Schema | Flyway, run only by ingest-static. Migration files live in rag-store so tests in other modules can apply them | Versioned, reviewable schema; no race between services |
| Re-index | One vector table per embedding model plus `index_registry` with the active one | Story 1 Task 4 needs a re-index with a different model. Switching is one transaction, so there's no downtime |
| Neighbors | retrieval-pre serves `GET /chunks/neighbors`; retrieval-post calls it | Only ingestion and retrieval-pre touch the vector tables |
| Shared code | `rag-common` (contracts), `rag-platform` (Spring setup), `rag-store` (pgvector), `rag-test-support` (fakes) | One definition of contracts and metadata keys |
| Eval | Separate `eval` module, owned by the Story 4 person | Keeps test-harness code out of the runtime images |
| Eval gate | The eval runs locally; CI checks the committed report's pipeline hash and fails on a drop of more than 0.02 in recall@k, MRR, refusal accuracy or citation faithfulness | Real models can't run in CI at useful quality |
| Base scope | Walking skeleton: every endpoint exists with a trivial implementation, so `/api/ask` works end to end on day one | Owners replace stubs instead of designing wiring |
| ingest-dynamic stub | Returns 501 | No URL fetching before the SSRF guard exists |
| Dev loop | `make dev svc=X` runs X on the host and the rest in Docker; `make up` for the full stack | Fast feedback and direct logs, which also suits coding agents |
| Formatting | Spotless with palantir-java-format | |
| Tests | JUnit 5, AssertJ, Mockito; Testcontainers pgvector and fake models; no Ollama in CI | Fast and deterministic |
| Git flow | Feature branch, PR, 1 approval, green CI, squash merge | Professor's baseline requires peer review |
| CI extras | JaCoCo summary without a threshold, gitleaks, PR template, CODEOWNERS, Dependabot | |
| Task runner | Makefile | Preinstalled on Linux and macOS |
| Observability | `grafana/otel-lgtm` behind compose profile `obs`; services export OTLP; two dashboards (service health, RAG pipeline) | Promtail is end of life; one container instead of five |
| Logging | Plain console lines with trace and span ids; the same events go to Loki over OTLP | Readable for people and agents; meets "errors and key events are logged" |
| Secrets | `.env` requires `POSTGRES_PASSWORD` and `VENDOR_WEBHOOK_SECRET`. The professor's `LLM_API_KEY` is dropped | Ollama needs no key |
| Package | groupId `ro.iapc`, packages `ro.iapc.rag.<module>` | |
| Agent tooling | `AGENTS.md` (source), `CLAUDE.md` imports it; skills in `.claude/skills/` | The team uses Claude Code and OpenCode |
| Ports | Fixed: rag-api 8081, ingest-static 8082, ingest-dynamic 8083, retrieval-pre 8084, retrieval-post 8085 | Predictable in host mode and logs |
| Lombok | Not used; records and constructors instead | One less annotation processor on Boot 4 |
| Service calls | `ServiceClients` adds the load-balancer interceptor to a private builder | A `@LoadBalanced RestClient.Builder` bean is picked up by Eureka's own HTTP client, which then can't register |

## Deviations from the professor's notes

- `LLM_API_KEY` is not required (Ollama).
- Flyway creates the schema instead of Spring AI's `initialize-schema`.
- One vector table per embedding model instead of a single `vector_store`, and no `EMBEDDING_DIMENSIONS` variable.
- A seventh service, `rag-api`, owns the public `/api/ask`.
- Versions are the latest patches of the lines he chose.

## Later entries

- 2026-11-22. All prose in the repo goes through the `unslop` skill, and every change updates the matching page in
  `docs/` in the same commit. Both rules are in `AGENTS.md`. The repo copy of `unslop` drops
  `disable-model-invocation` so agents can load it themselves.
