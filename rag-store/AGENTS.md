# rag-store (infra)

pgvector access shared by ingest-static, ingest-dynamic and retrieval-pre.

- `IndexRegistry`: the `index_registry` table, which vector table is active (cached `rag.store.registry-cache-ttl`).
- `VectorStoreProvider`: a Spring AI `PgVectorStore` per table; `createIndex(model)` creates `vector_store_<model>`
  with Spring AI's own DDL; `ensureActiveIndex()` on first start.
- `EmbeddingModelFactory` / `OllamaEmbeddingModelFactory`: one embedding model per model name. Tests replace it.
- `ChunkQueries`: metadata lookups a similarity search can't do (index ranges, counts). Raw SQL here must skip rows
  with `staged_version`, because the `is_latest` filter doesn't apply to it.
- `ChunkWriter`: the only way ingestion writes chunks. `write(index, docId, chunks, Retire.DELETE | FLAG)` stages the
  chunks hidden, then retires the old ones and publishes the new ones in one transaction (docs/architecture.md,
  "Writing a document version"). It logs at DEBUG only; the caller logs the ingestion once at INFO with the counts
  from `Result`.
- `ChunkMapper`: Spring AI `Document` to `Chunk`.
- `ManifestRepository` / `ManifestEntry`: the `ingestion_manifest` table, written by both ingestion services.
  `source_type` says which one. Story 1 owns the schema.
- `src/main/resources/db/migration`: Flyway migrations, run by ingest-static only. New file per change
  (`V2__...sql`); never edit a merged one. The Story 1 owner reviews these.
- `rag.store.models` defaults live in `RagStoreProperties`.

`VectorStoreProviderIT` covers creation, search, neighbors and switching the active index. `ManifestRepositoryIT`
covers the manifest. `ChunkWriterIT` covers both retire modes, staged chunks staying invisible, a failed embedding and
a rolled-back publish.
