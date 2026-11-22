# Base setup (2026-11-22)

This page records what the base setup put in the repository on branch `chore/base-setup`, and how it was checked.
The reasons behind each choice are in [decisions.md](decisions.md). Keep this page accurate when you change any
part of the setup.

## Starting point

The repository held the class demo in a nested `IAPC_repo/` folder. It was one Spring Boot 3.3 app using Spring AI
1.0.0-M6, Ollama and Qdrant, with `target/` and `.idea/` committed and no tests. The setup deleted it (it stays in
git history) and moved its six PDFs to `resources/`. The professor's task list moved to `docs/epic-1-tasks.md`.

## Build

The root `pom.xml` is a Maven multi-module build on Spring Boot 4.0.10, Spring Cloud 2025.1.5 and Spring AI 2.0.3,
with Java 21. These versions were checked on Maven Central. Spring AI 2.0.3 and Spring Cloud 2025.1.5 are both
built against Boot 4.0.10. The parent pom also configures:

- Spotless with palantir-java-format (`make fmt`, `make fmt-check`),
- surefire for `*Test` and failsafe for `*IT`,
- JaCoCo, merging unit and integration coverage per module.

The Maven wrapper pins Maven 3.9.16, so nobody needs Maven installed.

## Modules

| Module | Port | Contents |
|---|---|---|
| `eureka-server` | 8761 | Single-node registry with self-preservation off |
| `api-gateway` | 8080 | Routes `/api/**` to rag-api and `/<service-id>/**` to every service registered in Eureka |
| `rag-api` | 8081 | `POST /api/ask`. Calls retrieval-pre, then retrieval-post, and records per-stage timings |
| `ingest-static` | 8082 | Story 1. Page-based PDF ingestion, delete by `doc_id`, the manifest, Flyway, and creation of the first vector index on startup |
| `ingest-dynamic` | 8083 | Story 2. `POST /sources` and `POST /webhooks/vendor` return 501 until the SSRF guard exists |
| `retrieval-pre` | 8084 | Story 3. `POST /retrieve` runs a plain similarity search; `GET /chunks/neighbors` returns adjacent chunks |
| `retrieval-post` | 8085 | Story 4. `POST /answer` builds cited context blocks and calls the chat model |
| `eval` | none | Command-line harness that runs the golden set through the gateway |
| `rag-common` | none | Request and response records, `MetadataKeys`, `ServiceNames`, `MetricNames` |
| `rag-platform` | none | Shared Spring setup: logging, OTLP export, error handling, `ServiceClients`, `StageTimer`, the `hostdev` profile |
| `rag-store` | none | `IndexRegistry`, `VectorStoreProvider`, `ChunkQueries`, Ollama embedding models, Flyway migrations |
| `rag-test-support` | none | `FakeEmbeddingModel`, `FakeChatModel`, `FakeModels`, `PgvectorTestcontainer` |

The story services are a walking skeleton. Each endpoint exists and works end to end with the simplest
implementation, and each owner replaces the stub with their tasks. [open-items.md](open-items.md) lists what each
stub does today, and `grep -rn "TODO(story-" --include=*.java .` finds the places in code.

## Storage

pgvector holds one vector table per embedding model and an `index_registry` table that marks the active one, so a
re-index with another model builds a new table and switches in one transaction. Flyway migration `V1` creates the
extensions, `index_registry` and `ingestion_manifest`. Only ingest-static runs it. Details are in
[architecture.md](architecture.md).

## Running

- `compose.yaml` runs pgvector, Eureka, the gateway and the five services, with healthchecks and the professor's
  startup order. Only port 8080 is public; Postgres and Eureka bind to 127.0.0.1. Two profiles are optional.
  `ollama-gpu` runs Ollama with NVIDIA passthrough, and `obs` runs `grafana/otel-lgtm` (Grafana, Prometheus, Loki,
  Tempo).
- `compose.dev.yaml` publishes each service on 127.0.0.1 for `make dev`.
- One `Dockerfile` builds every service image from the same build stage. The image runs as a non-root user.
- The `Makefile` wraps everything (`make help`).
- `.env.example` lists the settings. Only `POSTGRES_PASSWORD` and `VENDOR_WEBHOOK_SECRET` are required.

[development.md](development.md) explains the setup and the run modes.

## Observability

Services log to the console with `[app,traceId,spanId]` on every line. With `make up-obs` they also export metrics,
traces and logs over OTLP. `scripts/gen-dashboards.py` generates two Grafana dashboards in
`observability/grafana/dashboards`, one for service health and one for the RAG pipeline.

## Evaluation

`eval/data/golden-set.json` has 14 starter questions, 3 of them unanswerable. `make eval` writes
`eval/reports/latest.json` with recall@k, MRR, refusal accuracy and a citation faithfulness check. The report also
stores the hash from `scripts/pipeline-hash.py`. That hash covers the pipeline modules' main code, every
`pom.xml`, `resources/` and the golden set. `eval/baseline.json` is the reference CI compares against.

The committed baseline comes from a retrieval-only run with `nomic-embed-text`, with recall@5 = 1.0 and
MRR = 0.909. With only six documents, recall@5 is close to guaranteed. A full run with `qwen2.5:7b` is still to do
(see [open-items.md](open-items.md)).

## CI and repository files

| File | What it does |
|---|---|
| `.github/workflows/ci.yml` | Checks formatting, runs `mvn verify`, posts a coverage table, validates the Compose files, checks the dashboards match their generator |
| `.github/workflows/gitleaks.yml` | Scans the history for secrets (`.gitleaks.toml` allowlists `.env.example` and test sources) |
| `.github/workflows/eval-check.yml` | Runs `scripts/eval-check.py`. It fails when the report's hash doesn't match the code or a metric drops more than 0.02 below the baseline |
| `.github/dependabot.yml` | Weekly updates for Maven, Actions and the Docker base images. Spring artifacts come grouped in one PR |
| `.github/CODEOWNERS` | Placeholders only, all commented out until the owners are known |
| `.github/pull_request_template.md` | The definition-of-done checklist |

Branch protection isn't enabled yet. [branch-protection.md](branch-protection.md) has the settings.

## Agent setup

`AGENTS.md` holds the rules, `CLAUDE.md` imports it, and each module has a short `AGENTS.md`. The skills are in
`.claude/skills/`:

| Skill | Use |
|---|---|
| `unslop` | Required for all prose in the repo |
| `dev-stack` | Running, inspecting and debugging the stack |
| `spring-ai-2-reference` | Spring AI 2.0, Boot 4 and Spring Cloud 2025.1 APIs, checked against the jars |
| `definition-of-done` | Checklist before opening or approving a PR |
| `eval-regression` | Running the eval, moving the baseline, fixing eval-check |

## How it was checked

- `./mvnw spotless:check verify` passes. That covers the unit tests in every module and the Testcontainers
  integration tests in rag-store, ingest-static and retrieval-pre.
- `make up` brought all containers to healthy, and every service registered in Eureka.
- With an Ollama container on CPU (`nomic-embed-text`, `qwen2.5:0.5b`), ingest-static ingested the six PDFs, and
  `POST /api/ask` through the gateway returned a cited answer.
- With the `obs` profile, Prometheus received the HTTP, JVM and `rag_*` metrics, and Loki received logs with trace
  ids. Tempo showed one trace spanning api-gateway, rag-api, retrieval-pre and retrieval-post. Both dashboards
  loaded.
- `make dev svc=retrieval-pre` started the service on the host, and it registered in Eureka as
  `host.docker.internal`. The gateway couldn't reach it on the test machine, because ufw drops traffic from
  containers to the host. [development.md](development.md) gives the firewall rules.
- `make eval-retrieval`, `make eval-baseline` and `make eval-check` ran against the live stack.
- gitleaks found no secrets.

## Problems found while building it

- A `@LoadBalanced RestClient.Builder` bean stopped every service except the gateway from registering in Eureka.
  Eureka's own HTTP client used the bean and tried to look up the Eureka server through Eureka.
  `ServiceClients` now adds the load-balancer interceptor to a private builder.
- `make dev svc=ingest-static` would have restarted the ingest-static container, because the other services depend
  on it. The Makefile now starts the rest with `--no-deps`.
- A similarity search with threshold 0 excludes chunks with similarity exactly 0, because PgVectorStore filters on
  `distance < 1`. The fake embedding model gives 0 to texts that share no words, and the tests account for that.
