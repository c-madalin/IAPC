# rag-test-support (infra)

Test-only helpers, used with test scope. `FakeEmbeddingModel`, `FakeChatModel`, `FakeModels` (import into a Spring
test) and `PgvectorTestcontainer` (import for a pgvector DataSource). Usage: docs/testing.md. It must not depend on
rag-store (rag-store's tests depend on it).
