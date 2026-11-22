# Agent guide

Every agent and contributor reads this file before changing anything. Claude Code loads it through `CLAUDE.md`, and
OpenCode reads it directly. Each module also has its own `AGENTS.md` with its owner, contract and task checklist.

## Writing: use unslop, always

Load the `unslop` skill (`.claude/skills/unslop/SKILL.md`) before you write or edit any prose, and apply it to all
of it. That covers docs, this file, READMEs, module `AGENTS.md` files, code comments, commit messages, PR
descriptions and replies to the user. There are no exceptions for short text or drafts. The professor's text in
`docs/epic-1-tasks.md` is the only prose you leave as it is.

## Documentation must be updated on every change

The documentation lives in [docs/](docs/README.md). Every change that alters behaviour, configuration, commands,
contracts, layout or decisions updates the matching doc in the same commit. A PR without its doc update is not done.

| If you change | Update |
|---|---|
| A service, module or the request flow | `docs/architecture.md`, the module's `AGENTS.md` |
| A record in `rag-common` or an endpoint | `docs/contracts.md` |
| A setting or env var | `docs/configuration.md`, `.env.example` |
| Make targets, Compose, ports, the dev loop | `docs/development.md` |
| Test helpers or test conventions | `docs/testing.md` |
| A decision, or a deviation from the professor's notes | `docs/decisions.md` (add a dated entry) |
| A placeholder, stub or open question (added or resolved) | `docs/open-items.md` |
| CI workflows or repo settings | `docs/base-setup.md`, `docs/branch-protection.md` |

`docs/base-setup.md` records what the base setup contains. Keep it accurate when you change any part of it.

## How to work

1. Don't assume. When a choice belongs to the user or the team (scope, naming, versions, ownership, numeric
   targets), ask. Give a recommendation and explain the trade-off. If you pick a default yourself, say so and
   present it as a proposal. Anything still undecided goes into `docs/open-items.md`.
2. Check facts before you build on them. Look up versions on Maven Central and read the jar sources for Spring AI
   or Boot APIs. Don't rely on memory or on unverified notes, the professor's included. He wrote that he couldn't
   check his versions and starter names. `.claude/skills/spring-ai-2-reference/SKILL.md` lists the APIs already
   checked.
3. A green build doesn't prove the wiring works. After changing service wiring, Compose, the gateway or Eureka
   setup, run the stack and send a request through the gateway. The `dev-stack` skill explains how.
4. Report what happened. State what you verified and how, what you didn't verify, and what failed, with the
   output. Don't describe a step as done if you skipped it.
5. Ask before anything hard to undo or visible to others. That includes commits, pushes, PRs, branch settings,
   `make nuke`, deleting volumes and changing firewall rules. Work on a feature branch, never on `main`.
6. Clean up after yourself. Remove containers, volumes, networks and `.env` files you created for a test, and
   stop processes you started.
7. Stay in your module. A task belongs to one service. If it needs a change in a shared module, Compose, CI or
   another service, make that a separate small commit and call it out in the PR.
8. Before opening a PR, walk through `.claude/skills/definition-of-done/SKILL.md`.

## Commands

| Goal | Command |
|---|---|
| Format, before every commit | `make fmt` |
| Unit tests, no Docker | `make test`, or `./mvnw -pl <module> -am test` |
| Unit and integration tests (Testcontainers) | `make it` |
| What CI runs | `make verify` |
| One service on the host, the rest in Docker | `make dev svc=<module>` |
| Full stack | `make up`, `make logs s=<service>`, `make down` |
| Eval against the running stack | `make eval`, `make eval-retrieval`, `make eval-check` |

Add `-am` when you build one module, so Maven rebuilds the shared modules it depends on.

## Stack facts that models get wrong

- The versions are Spring Boot 4.0.10, Spring Cloud 2025.1.5, Spring AI 2.0.3 and Java 21. Most training data
  covers Boot 3 and Spring AI 1.x or its milestones, and those APIs differ.
- Jackson 3 lives in `tools.jackson.*`. Its annotations stay in `com.fasterxml.jackson.annotation`.
- Boot 4 split the starters into `spring-boot-starter-webmvc`, `-restclient`, `-flyway` and so on. Test slices moved
  too, for example to `org.springframework.boot.webmvc.test.autoconfigure`.
- Gateway properties sit under `spring.cloud.gateway.server.webflux.*`.
- The models run on Ollama, not OpenAI. Code builds them (`OllamaEmbeddingModelFactory`, `ChatModelConfiguration`).
  The Spring AI starters' auto-configuration is not used.

## Code rules

1. The records in `rag-common` are the API between services. When you change one, update every caller, their
   tests and `docs/contracts.md` in the same PR. Add fields freely. Ask the team before renaming or removing one.
2. Take metadata keys from `MetadataKeys`. Never type `"doc_id"` as a literal.
3. Schema changes are new Flyway files in `rag-store/src/main/resources/db/migration`, named `V<n>__<what>.sql`.
   Never edit a merged migration. `VectorStoreProvider` creates the vector tables at runtime, one per embedding
   model, so don't create them in SQL.
4. Call other services through `ServiceClients.forService(ServiceNames.X)`. Never declare a `@LoadBalanced
   RestClient.Builder` bean. Eureka's own client picks it up and then can't register.
5. Throw `IllegalArgumentException` for bad input (400) and `ResponseStatusException` for other expected statuses.
   `ApiExceptionHandler` turns them into problem responses and logs them. Don't catch and swallow exceptions.
6. Log each key event once with SLF4J (document ingested, query answered, stage failed), with ids and counts. Don't
   log document text, full prompts or secrets at INFO.
7. Time pipeline stages with `StageTimer` and name metrics from `MetricNames`. The Grafana RAG dashboard queries
   those names.
8. Every change needs at least one success-path test and one failure-path test. Use the fakes in
   `rag-test-support`. Tests never call real Ollama or the internet. See `docs/testing.md`.
9. A new setting gets a `${ENV_VAR:default}` property in the module's `application.yaml`, an entry in
   `.env.example` if people will change it, and a row in `docs/configuration.md`. No secrets in the repo.
10. Format with `make fmt` (Palantir Java Format). Use records and constructor injection; the repo doesn't use
    Lombok. Comments explain why, not what. Write TODOs as `TODO(story-N-task-M): ...` so `grep` finds them.
11. Changing main code in a pipeline module, a `pom.xml`, the PDFs in `resources/` or the golden set changes the
    pipeline hash. CI then fails until you run `make eval` and commit `eval/reports/latest.json`. The
    `eval-regression` skill has the details.
