# Development

## One-time setup

1. JDK 21 (`java -version`). Maven isn't needed; use `./mvnw`.
2. Docker with Compose v2.20 or newer (`docker compose version`).
3. Ollama. On macOS, and on any machine where you'd rather not containerize it, install Ollama natively and run
   `make models`. That pulls `nomic-embed-text` for embeddings and `qwen2.5:7b` for chat (about 4.7 GB). On an
   NVIDIA machine you can run it in a container instead. Set `OLLAMA_BASE_URL=http://ollama:11434` in `.env` and
   start with `docker compose --profile ollama-gpu up -d`. The `ollama-pull` job downloads the models. This needs
   the NVIDIA Container Toolkit.
4. `make env`, then put a password in `POSTGRES_PASSWORD` and any random string in `VENDOR_WEBHOOK_SECRET`.
5. IntelliJ: open the root `pom.xml` as a project. Install the "palantir-java-format" plugin so the IDE formats like
   `make fmt`.

### Linux firewall (ufw, firewalld)

Containers reach your machine through `host.docker.internal`. They need that for host Ollama and for `make dev`.
Many Linux firewalls drop this traffic, which shows up as "Connection timed out" to `host.docker.internal` in the
service logs. Allow the Docker networks to reach the ports involved:

```bash
sudo ufw allow from 172.16.0.0/12 to any port 11434 proto tcp        # host Ollama
sudo ufw allow from 172.16.0.0/12 to any port 8081:8085 proto tcp    # make dev
```

macOS and Docker Desktop don't need this.

## Ways to run

| Command | What runs where | Use it for |
|---|---|---|
| `make test` / `make it` | Nothing long-running; Testcontainers starts a throwaway Postgres | Most work. Fastest loop |
| `make dev svc=X` | X on your machine (`spring-boot:run`, profile `hostdev`), everything else in Docker | Working on one service against the real stack |
| `make up` | Everything in Docker | Demos, `make eval`, checking the full flow |
| `make up-obs` | Same plus Grafana/Prometheus/Loki/Tempo at http://localhost:3000 | Latency and tracing work |

`make dev` stops X's container, starts the rest with the `compose.dev.yaml` overlay (which publishes every service
on 127.0.0.1), builds X and runs it in the foreground. Ctrl-C stops it. In `hostdev`:

- X registers in Eureka as `host.docker.internal`, so the gateway container can route to it.
- X calls other services at `localhost:<port>` (see `rag-platform/src/main/resources/application-hostdev.properties`).
- X uses Postgres on `localhost:5432` and Ollama on `localhost:11434` (override with `OLLAMA_HOST_URL`).

After you stop it, Eureka keeps the host registration for up to about 30 seconds. Requests through the gateway can
fail during that window.

## Ports

| Port | Service | Published |
|---|---|---|
| 8080 | api-gateway | yes, the only public port |
| 8761 | eureka-server (dashboard) | 127.0.0.1 |
| 5432 | pgvector (user `rag`, db `rag`) | 127.0.0.1 |
| 3000 | Grafana (`obs` profile) | 127.0.0.1 |
| 8081 to 8085 | rag-api, ingest-static, ingest-dynamic, retrieval-pre, retrieval-post | only with `make dev` |

## Useful requests

```bash
curl -X POST localhost:8080/api/ask -H 'Content-Type: application/json' -d '{"query":"Is MFA mandatory?"}'
curl -X POST localhost:8080/retrieval-pre/retrieve -H 'Content-Type: application/json' -d '{"query":"MFA","topN":5}'
curl -X POST localhost:8080/ingest-static/documents/sync
curl localhost:8080/ingest-static/manifest
curl localhost:8761/eureka/apps -H 'Accept: application/json'     # what's registered
```

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Gateway returns 503 | The target service isn't registered yet (it takes up to ~30 s after start) or is down. Check `make ps` and the Eureka dashboard |
| "Connection timed out" to `host.docker.internal` | Firewall, see above. Or Ollama isn't running: `curl localhost:11434/api/tags` |
| `No active vector index` | ingest-static hasn't started yet; it creates the index on startup |
| Compose: `set POSTGRES_PASSWORD in .env` | `.env` is missing or the value is empty |
| Password errors after changing `POSTGRES_PASSWORD` | The volume keeps the old password. `make nuke` deletes the database |
| Spotless fails in CI | Run `make fmt` and commit |
| eval-check fails | Pipeline code changed; run `make eval` and commit the report |
