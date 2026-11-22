# ingest-static (Story 1: static ingestion)

Owner: `<story-1-owner>` (see docs/open-items.md). Port 8082. Package `ro.iapc.rag.ingeststatic`.

Reads PDFs from `rag.ingest.resources` (`./resources` mounted read-only in Docker), chunks and embeds them, and
writes them to the active vector table. It is the only service that runs Flyway (migrations are in
`rag-store/src/main/resources/db/migration`) and it creates the default vector index on startup, so the other
services wait for it to be healthy.

## Code

- `PdfIngestionService`: the pipeline. Skeleton: page reader, `TokenTextSplitter`, delete-then-add per `doc_id`.
- `ManifestRepository`: `ingestion_manifest` table.
- `StartupIndexer`: `ensureActiveIndex()` then the optional sync on startup.
- `DocumentController`: `POST /documents/sync`, `DELETE /documents/{docId}`, `GET /manifest`.
- From rag-store: `VectorStoreProvider` (stores per table, `createIndex`), `IndexRegistry` (`activate`),
  `ChunkQueries` (counts, ranges).

## Tasks (full criteria in docs/epic-1-tasks.md)

- [ ] 1.1 Structure-aware chunking with overlap; tables and multi-column PDFs; full metadata on every chunk; chunk
  size, overlap and model configurable; re-run creates no duplicates; manual check of 20 random chunks.
- [ ] 1.2 Checksum comparison skips unchanged files; atomic replacement (queries never see a mix or nothing);
  failure mid-update keeps the previous version; test for the chunk count after update.
- [ ] 1.3 Delete by `doc_id`; deleted content no longer retrieved; orphan check reports zero; deleting a missing
  document is safe with a clear message (404 today).
- [ ] 1.4 Manifest records doc_id, checksum, version, timestamp, model, chunk count; reconciliation script shows zero
  drift; full re-index with a different embedding model on the sample set.

Hints: for atomic replacement, write the new chunks first and remove the old ones in one transaction (PgVectorStore
shares the `JdbcTemplate`, so a `TransactionTemplate` around both works), or use the version metadata. For re-index,
`VectorStoreProvider.createIndex(model)`, ingest into `forIndex(newIndex)`, reconcile, then `IndexRegistry.activate`.

## Tests

`DocumentControllerIT` (Testcontainers, fake embeddings) shows the pattern. The fixture PDF is in
`src/test/resources/pdfs/`; add the table and multi-column PDFs there too.
