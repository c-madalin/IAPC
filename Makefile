# Run `make help` for the list. Everything a teammate or an agent needs day to day goes through these targets.

SHELL := /bin/bash
MVN := ./mvnw -B
COMPOSE := docker compose
DEV_COMPOSE := docker compose -f compose.yaml -f compose.dev.yaml
APP_SERVICES := eureka-server api-gateway ingest-static ingest-dynamic retrieval-pre retrieval-post rag-api
svc ?=
s ?=
SERVICE_PORT_eureka-server := 8761
SERVICE_PORT_api-gateway := 8080
SERVICE_PORT_rag-api := 8081
SERVICE_PORT_ingest-static := 8082
SERVICE_PORT_ingest-dynamic := 8083
SERVICE_PORT_retrieval-pre := 8084
SERVICE_PORT_retrieval-post := 8085

.DEFAULT_GOAL := help
.PHONY: help env up up-obs down nuke ps logs dev build test it verify fmt fmt-check eval eval-retrieval eval-baseline eval-check models dashboards clean

help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

env: ## Create .env from .env.example if it doesn't exist
	@test -f .env || (cp .env.example .env && echo "Created .env. Set POSTGRES_PASSWORD and VENDOR_WEBHOOK_SECRET.")

up: env ## Build and start the full stack in Docker
	$(COMPOSE) up --build -d
	@echo "Gateway: http://localhost:8080  Eureka: http://localhost:8761"

up-obs: env ## Full stack plus Grafana/Prometheus/Loki/Tempo (http://localhost:3000)
	OTEL_ENABLED=true $(COMPOSE) --profile obs up --build -d

down: ## Stop the stack (keeps the database volume)
	$(COMPOSE) --profile obs --profile ollama-gpu down

nuke: ## Stop the stack and delete volumes (database, Ollama models)
	$(COMPOSE) --profile obs --profile ollama-gpu down -v

ps: ## Show containers and health
	$(COMPOSE) ps

logs: ## Follow logs, all services or one: make logs s=retrieval-pre
	$(COMPOSE) logs -f --tail=200 $(s)

dev: env ## Run one service on the host, the rest in Docker: make dev svc=retrieval-pre
	@test -n "$(svc)" || (echo "usage: make dev svc=<$(APP_SERVICES)>" && exit 1)
	@echo "$(APP_SERVICES)" | tr ' ' '\n' | grep -qx "$(svc)" || (echo "unknown service $(svc)" && exit 1)
	-$(COMPOSE) rm -sf $(svc)
	@# --no-deps: otherwise Compose would start $(svc)'s container again as a dependency of the others.
	$(DEV_COMPOSE) up --build --no-deps -d pgvector $(filter-out $(svc),$(APP_SERVICES))
	$(MVN) -q -pl $(svc) -am install -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true
	set -a; source .env; set +a; \
	  export SPRING_PROFILES_ACTIVE=hostdev SERVER_PORT=$(SERVICE_PORT_$(svc)) \
	         OLLAMA_BASE_URL=$${OLLAMA_HOST_URL:-http://localhost:11434} \
	         INGEST_RESOURCES=file:$(CURDIR)/resources/*.pdf; \
	  $(MVN) -pl $(svc) spring-boot:run

build: ## Compile and package everything, no tests
	$(MVN) -q package -DskipTests

test: ## Unit tests (*Test), no Docker needed
	$(MVN) test

it: ## Unit + integration tests (*IT, Testcontainers; needs Docker)
	$(MVN) verify

verify: fmt-check it ## What CI runs: formatting check, then all tests

fmt: ## Format Java sources (Palantir style)
	$(MVN) -q spotless:apply

fmt-check: ## Fail if any file isn't formatted
	$(MVN) -q spotless:check

eval: ## Run the golden set through the running stack; writes eval/reports/latest.json
	$(MVN) -q -pl eval -am install -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true
	set -a; source .env; set +a; \
	  java -jar eval/target/eval-*.jar --eval.pipeline-hash=$$(scripts/pipeline-hash.py) \
	    --eval.chat-model=$${CHAT_MODEL:-qwen2.5:7b} $(ARGS)

eval-retrieval: ## Like eval, but retrieval metrics only (no LLM calls)
	$(MAKE) eval ARGS="--eval.retrieval-only=true $(ARGS)"

eval-baseline: ## Promote the latest report to the baseline CI compares against
	cp eval/reports/latest.json eval/baseline.json
	@echo "eval/baseline.json updated. Commit it with a note on why the baseline moved."

eval-check: ## Run the CI eval gate locally
	scripts/eval-check.py

models: ## Pull the default models into host Ollama
	ollama pull $${EMBEDDING_MODEL:-nomic-embed-text}
	ollama pull $${CHAT_MODEL:-qwen2.5:7b}

dashboards: ## Regenerate Grafana dashboards from scripts/gen-dashboards.py
	scripts/gen-dashboards.py

clean: ## Remove build output
	$(MVN) -q clean
