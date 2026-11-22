# rag-ingest (Story 1, shared)

Owner: `<story-1-owner>`. Package `ro.iapc.rag.ingest`. A plain library with no Spring configuration. Only
ingest-static and ingest-dynamic depend on it, so the retrieval services don't carry PDF libraries.

Chunking lives here so local PDFs (Story 1) and remote PDFs (Story 2 Task 1) come out the same, and the eval numbers
don't depend on which service ingested a document.

- `PdfChunker`: PDF to chunks with `page` and `chunk_index`. Skeleton: page reader and `TokenTextSplitter` without
  overlap. Story 1 Task 1 replaces it with structure-aware chunking with overlap.
- `ChunkMetadata`: the document-level keys (`doc_id`, `version`, `source`, `checksum`, `is_latest`,
  `embedding_model`), written onto each chunk.
- `Checksums`: SHA-256 hex of a resource or bytes.

Changing main code here changes the pipeline hash, so run `make eval` (see the `eval-regression` skill).

## Tests

Plain JUnit, no Spring and no Docker. The fixture PDF is in `src/test/resources/pdfs/`.
