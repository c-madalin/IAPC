# Testing

Every change needs at least one test for the success path and one for a failure path (professor's baseline).

## Kinds of tests

| Name pattern | Runs in | Needs | Command |
|---|---|---|---|
| `*Test` | `mvn test` (surefire) | nothing | `make test` |
| `*IT` | `mvn verify` (failsafe) | Docker (Testcontainers) | `make it` |

CI runs both on every PR. Coverage (JaCoCo, unit plus integration) is in the job summary; there's no threshold.

## Building blocks (rag-test-support)

- `FakeEmbeddingModel`: deterministic word-hashing embedding. Texts that share words come out similar, texts that
  share none have similarity 0. Note that a similarity search with threshold 0 excludes chunks with no word in
  common with the query.
- `FakeChatModel`: returns a fixed answer or `respondWith(prompt -> ...)`, can `failWith(exception)`, and keeps
  every prompt for assertions (`lastPrompt()`). Each response carries token usage, by default the word counts of
  the prompt and the answer. `reportUsage(prompt, completion)` fixes the numbers, for testing a token cap.
  `reportNoUsage()` leaves usage out, and Spring AI then reports 0 tokens.
- `FakeModels`: `@Import` it to replace Ollama with both fakes in a Spring test.
- `PgvectorTestcontainer`: `@Import` it to get a pgvector Postgres wired as the DataSource. Flyway runs the
  migrations if the module has Flyway on its test classpath (retrieval-pre and rag-store do).

Services built on rag-store also declare the fake embedding factory:

```java
@TestConfiguration
static class Fakes {
    @Bean
    EmbeddingModelFactory fakeEmbeddings(RagStoreProperties properties) {
        return model -> new FakeEmbeddingModel(properties.dimensionsOf(model));
    }
}
```

## Patterns in this repo

- HTTP endpoint against a real database: `retrieval-pre/.../RetrievalControllerIT`, `ingest-static/.../DocumentControllerIT`.
- Calls to other services mocked with `MockRestServiceServer`: `rag-api/.../AskControllerTest`.
- LLM behaviour with the fake chat model: `retrieval-post/.../AnswerControllerTest`.
- A failing embedding model: `rag-store/.../ChunkWriterIT` replaces the factory with a `@Primary` one whose fake
  throws when a flag is set.
- Pure logic, no Spring: `eval/.../MetricsTest`, `rag-platform/.../StageTimerTest`.

Prefer plain unit tests for logic (normalization rules, filter parsing, RRF, stitching) and keep Spring tests for
wiring and HTTP behaviour. Use `MockMvcTester` with AssertJ for MVC tests.

## Not in CI

Real models. Answer quality is measured by the eval harness (`make eval`) against the running stack, and CI only
checks that the committed report is current. See `.claude/skills/eval-regression/SKILL.md`.
