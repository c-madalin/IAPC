# ingest-dynamic (Story 2: dynamic ingestion)

Owner: `<story-2-owner>`. Port 8083. Package `ro.iapc.rag.ingestdynamic`.

Ingests content from the internet (pages, remote files, third-party repository docs), keeps old versions, reacts
to file changes and vendor webhooks. Today both endpoints return 501.

**Security first.** Don't merge any code that fetches a URL until the SSRF guard (Task 4) is in and tested: block
localhost, private ranges (10/8, 172.16/12, 192.168/16, 169.254/16 including the cloud metadata address
169.254.169.254, IPv6 loopback and ULA), resolve DNS once and connect to the checked IP, refuse redirects to
blocked hosts, HTTPS only, domain allowlist, timeout, size limit, content-type check.

## Code

- `SourceController`: `POST /sources`, `POST /webhooks/vendor` (501 stubs).
- Config: `rag.dynamic.webhook-secret` (`VENDOR_WEBHOOK_SECRET`), `rag.dynamic.block-private-networks`.
- Chunk remote PDFs with rag-ingest's `PdfChunker` and set the document keys with `ChunkMetadata`, so they match
  static ones. Doc ids are `web:<normalized URL>` and `repo:<owner>/<repo>/<path>` (docs/decisions.md).
- Record every ingested version with rag-store's `ManifestRepository`, `SourceType.DYNAMIC`.
- Write chunks with rag-store's `ChunkWriter` and `Retire.FLAG`, which keeps old versions with `is_latest=false`.
  Set `valid_from` yourself.
- Writes through rag-store's `VectorStoreProvider.active()` like ingest-static; use `MetadataKeys.SOURCE_URL`,
  `FETCHED_AT`, `VERSION`, `IS_LATEST`, `VALID_FROM`.

## Tasks (full criteria in docs/epic-1-tasks.md)

- [ ] 2.1 Web page, remote PDF and one repo's docs ingested; boilerplate removed; URL and fetch date in metadata;
  one failed source logged without stopping the batch.
- [ ] 2.2 New version keeps old chunks with `is_latest=false` and `valid_from`; default query returns latest only;
  explicit version returns older content; test with two versions. This needs retrieval-pre to filter on
  `is_latest` by default; coordinate with the Story 3 owner.
- [ ] 2.3 Watcher detects add/change/delete within 30 s; webhook verifies HMAC and returns 401 on a bad signature;
  valid notification triggers ingestion; duplicates deduped; failed jobs retry with backoff and are logged.
- [ ] 2.4 SSRF and the limits above, script stripping, documented negative tests for each control.

## Tests

Add Spring AI's `spring-ai-jsoup-document-reader` if you use it (it's in the BOM). Test the SSRF guard as plain unit
tests with a table of blocked and allowed URLs. Never hit the real internet from tests: use a local HTTP server
(for example `com.sun.net.httpserver.HttpServer`) and allowlist it explicitly in the test.
