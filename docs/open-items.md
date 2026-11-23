# Open items and placeholders

Everything still undecided or stubbed, in one place. When you resolve one, delete it here and note the decision in
`docs/decisions.md` if it's a decision.

## To decide or provide

| Item | Where it shows up | Owner |
|---|---|---|
| Story owners and their GitHub handles | `.github/CODEOWNERS` is fully commented out with `<story-N-owner>` and `<infra-owner-N>` placeholders | team |
| Who takes two stories | README module table says "Story N" only | team |
| Branch protection | Not enabled yet. Settings in `docs/branch-protection.md`; needs the repo admin (c-madalin) | repo admin |
| Sample PDFs with a table and with multiple columns | Story 1 Task 1 needs both in `resources/`. Table and multi-column fixtures are available for the chunking tests | Story 1 |
| First full eval report and baseline | `eval/baseline.json` currently comes from a retrieval-only run (no chat model). Run `make eval` with `qwen2.5:7b` on a GPU machine, then `make eval-baseline`, so refusal and faithfulness are gated too | Story 4 |
| Numeric targets to agree with the product owner | Watcher latency (30 s proposed), ReDoS time limit, multi-query latency and token caps, similarity threshold, eval tolerance (0.02) | each story owner |
| Second embedding model for the re-index demo | `mxbai-embed-large` (1024 dims) is listed in `rag.store.models` as a candidate | Story 1 |
| Golden set size | 14 starter questions in `eval/data/golden-set.json`; Story 4 Task 4 needs at least 30 including unanswerable ones | Story 4 |
| OR across different filter fields | The bracket syntax (decisions, 2026-11-22) has OR only within one field. Ask the product owner whether Story 3 Task 1's "AND/OR" needs `source=a OR page>5` | Story 3 |
| Where the cross-encoder runs | Story 4 Task 1. Ollama has no reranking API, so the options are ONNX Runtime or DJL inside retrieval-post, or a sidecar container (see retrieval-post's `AGENTS.md`) | Story 4 |
| Fewer than topN results with filters | pgvector's HNSW index filters after the index scan, so once old versions pile up, a default `is_latest` search can return fewer than `topN` chunks. Check `hnsw.ef_search` or iterative scans when it shows up | Stories 2 and 3 |
| Postman collection | Story 3 Task 1 requires one committed; no folder exists yet (suggest `docs/postman/`) | Story 3 |

## Stubs in the walking skeleton

`grep -rn "TODO(story-" --include=*.java .` lists them in code.

| Story / task | Stub today |
|---|---|
| 1.1 | The chunking implementation needs a 20-chunk manual text-quality check |
| 1.2 | Changed-PDF integration test and mid-update failure test need final coverage |
| 1.3 | Delete works; there's no orphan check |
| 1.4 | Manifest table and `/manifest` exist; no reconciliation script or re-index endpoint (the store API supports it) |
| 2.1 to 2.4 | ingest-dynamic endpoints return 501 |
| 3.1 | Any filter is rejected with 400. The default `is_latest == true` filter is in place |
| 3.2 | The query is only trimmed |
| 3.3, 3.4 | Not started; `stages.expand` is ignored |
| 4.1 | No reranker; `stages.rerank` is ignored |
| 4.2 | No stitching; the neighbors endpoint exists in retrieval-pre |
| 4.3 | Citation headers and a grounded prompt exist; no similarity threshold |
| 4.4 | Runner computes recall@k, MRR, refusal accuracy and a citation-in-context faithfulness check; an LLM-judge faithfulness check is still TODO |

## Known limitations of the setup

- On Linux with ufw or firewalld, containers can't reach host Ollama or a `make dev` service until the firewall
  allows it (see docs/development.md).
- `make dev` assumes `.env` exists and that the service's container can be stopped.
- The gateway exposes every registered service under `/<service-id>/**`. Fine for coursework; production needs
  explicit routes.
