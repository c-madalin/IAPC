---
name: dev-stack
description: Start, stop, inspect and debug the IAPC microservices stack (Docker Compose, Eureka, gateway, Ollama, pgvector), run one service on the host with make dev, read logs and traces. Use when asked to run the app, reproduce a bug against the real stack, check why a service is unhealthy or unreachable, or call an endpoint.
---

# Running and debugging the stack

All commands run from the repo root. `make help` lists targets. Details: docs/development.md.

## Pick the lightest option

1. Most changes need no stack: `./mvnw -pl <module> -am verify` runs unit tests and Testcontainers ITs.
2. One service against real neighbours: `make dev svc=<module>` (foreground; run it in the background if you need
   the terminal, and stop it when done). Other services run in Docker.
3. Everything in Docker: `make up`. With metrics, traces and Grafana: `make up-obs`.

Check `.env` exists first (`make env` creates it; the two required values must be set by a person, don't invent
secrets in a shared file).

## Health and registration

```bash
make ps                                                     # container health
curl -s localhost:8761/eureka/apps -H 'Accept: application/json' | jq '.applications.application[].name'
curl -s localhost:8080/<service-id>/actuator/health         # through the gateway
curl -s localhost:11434/api/tags                            # Ollama is up and has the models
```

A service is routable through the gateway only after it shows up in Eureka (up to ~30 s after start). A 503 from
the gateway means "not registered" more often than "down".

## Logs

`make logs s=<service>` (compose service names match module names). Each line has `[app,traceId,spanId]`. To follow
one request across services, copy the traceId and grep for it in all logs: `docker compose logs | grep <traceId>`.
With `make up-obs`, Grafana at http://localhost:3000 has Tempo (traces) and Loki (logs, label `service_name`).

## Common failures

| Symptom | Check |
|---|---|
| `Connection timed out` to `host.docker.internal` | Host firewall drops container traffic (Linux ufw/firewalld) or host Ollama isn't running. See docs/development.md, "Linux firewall". Don't change firewall rules yourself; tell the user the command |
| `No active vector index` | ingest-static hasn't finished starting |
| Gateway 503 for a service you run with `make dev` | It registered as `host.docker.internal`; the gateway container must reach your host on that port (firewall again) |
| `BeanCurrentlyInCreationException ... eurekaClient` | Someone added a `@LoadBalanced RestClient.Builder` bean. Use `ServiceClients` |
| Answers are "I don't know" for everything | Nothing ingested: `curl localhost:8080/ingest-static/manifest`, then `curl -X POST localhost:8080/ingest-static/documents/sync` |
| Flyway checksum mismatch | A merged migration was edited. Revert it and add a new `V<n>__` file |

## Resetting

`make down` stops containers and keeps the database. `make nuke` also deletes volumes (database and Ollama models).
Ask before running `make nuke` on someone's machine: it deletes their ingested data.
