# IAPC RAG

Question answering over internal PDFs, built as Spring Boot microservices for the IAPC course. Epic 1 (RAG) is due
2026-12-07. The professor's tasks are in [docs/epic-1-tasks.md](docs/epic-1-tasks.md).

## Run it

You need JDK 21, Docker with Compose 2.20 or newer, and Ollama.

```bash
make env       # creates .env; set POSTGRES_PASSWORD and VENDOR_WEBHOOK_SECRET
make models    # pulls nomic-embed-text and qwen2.5:7b into Ollama
make up        # builds and starts the stack, then ingests ./resources
curl -X POST localhost:8080/api/ask -H 'Content-Type: application/json' \
     -d '{"query":"What is the minimum password length?"}'
```

`make help` lists the other commands. `make dev svc=<module>` runs one service on your machine and the rest in
Docker.

## Documentation

Start at [docs/README.md](docs/README.md). Agents and contributors also read [AGENTS.md](AGENTS.md).
