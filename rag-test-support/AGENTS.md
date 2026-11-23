# rag-test-support (infra)

Test-only helpers, used with test scope. `FakeEmbeddingModel`, `FakeChatModel`, `FakeModels` (import into a Spring
test) and `PgvectorTestcontainer` (import for a pgvector DataSource). Usage: docs/testing.md. It must not depend on
rag-store (rag-store's tests depend on it). `FakeChatModel` reports token usage, so Story 3 Task 4 can test its
token cap with `reportUsage`.
