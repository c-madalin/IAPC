# Configuration

Each service reads `application.yaml`; values in `${VAR:default}` form come from the environment, so Compose (and
`.env`) can set them. Shared defaults for every service are in `rag-platform/src/main/resources/rag-platform-defaults.properties`.

The professor's notes used placeholder names like `APP_CHUNK_SIZE`; this table maps them to what the code reads.

| Env var | Property | Service | Default | Notes |
|---|---|---|---|---|
| `POSTGRES_PASSWORD` | `spring.datasource.password` | ingest-*, retrieval-pre | none, required | |
| `VENDOR_WEBHOOK_SECRET` | `rag.dynamic.webhook-secret` | ingest-dynamic | none, required | HMAC key for Story 2 Task 3 |
| `OLLAMA_BASE_URL` | `rag.store.ollama-base-url`, `rag.answer.ollama-base-url` | ingest-*, retrieval-* | `http://host.docker.internal:11434` in Docker | |
| `EMBEDDING_MODEL` | `rag.store.default-model` | ingest-*, retrieval-pre | `nomic-embed-text` | Model of the index created when none exists. Must be a key in `rag.store.models` |
| (code) | `rag.store.models` | ingest-*, retrieval-pre | `nomic-embed-text: 768`, `mxbai-embed-large: 1024` | Allowed embedding models and their dimensions. Defaults live in `RagStoreProperties` so every service agrees; add new models there |
| `CHAT_MODEL` | `rag.answer.chat-model` | retrieval-post | `qwen2.5:7b` | |
| `CHUNK_SIZE` (was `APP_CHUNK_SIZE`) | `rag.ingest.chunk-size` | ingest-static | `400` words | Target chunk size |
| `CHUNK_OVERLAP` | `rag.ingest.chunk-overlap` | ingest-static | `50` words | Repeated words between adjacent chunks |
| `INGEST_RESOURCES` | `rag.ingest.resources` | ingest-static | `file:./resources/*.pdf` | `/app/resources` in Docker, mounted read-only |
| `INGEST_SYNC_ON_STARTUP` | `rag.ingest.sync-on-startup` | ingest-static | `false`, `true` in Compose | |
| `RETRIEVAL_TOP_N` | `rag.retrieval.default-top-n` | retrieval-pre | `20` | Candidates before reranking |
| `RERANK_TOP_K` (was `APP_RERANK_TOP_K`) | `rag.answer.max-context-chunks` | retrieval-post | `5` | Chunks in the prompt |
| `INGEST_BLOCK_PRIVATE_NETWORKS` (was `APP_INGEST_BLOCK_PRIVATE_NETWORKS`) | `rag.dynamic.block-private-networks` | ingest-dynamic | `true` | Compose can't enforce this; the fetcher must |
| `OTEL_ENABLED` | `management.*.export.*.enabled` | all | `false` | `make up-obs` sets it |
| `OTEL_ENDPOINT` | OTLP endpoints | all | `http://localhost:4318` | `http://otel-lgtm:4318` in Docker |
| `EUREKA_URL` | `eureka.client.service-url.defaultZone` | all | `http://localhost:8761/eureka` | |
| `EUREKA_INSTANCE_HOSTNAME` | `eureka.instance.hostname` | all | `localhost` | The service name in Docker, `host.docker.internal` in `make dev` |
| `DB_URL` | `spring.datasource.url` | ingest-*, retrieval-pre | `jdbc:postgresql://localhost:5432/rag` | |

When you add a setting: put it in the module's `application.yaml` as `${ENV_VAR:default}`, add it to `.env.example`
if people will change it, pass it in `compose.yaml` if the container needs it, and add a row here.

## Embedding dimensions

The professor's `EMBEDDING_DIMENSIONS` doesn't exist here. Dimensions are per model in `rag.store.models`, and
each model gets its own table, so switching models means a re-index into a new table, not a schema change.
