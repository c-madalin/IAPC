# Open items and placeholders

Everything still undecided or stubbed, in one place. When you resolve one, delete it here and note the decision in
`docs/decisions.md` if it's a decision.

## To decide or provide

| Item | Where it shows up | Owner |
|---|---|---|
| Story owners and their GitHub handles | `.github/CODEOWNERS` is fully commented out with `<story-N-owner>` and `<infra-owner-N>` placeholders | team |
| Who takes two stories | README module table says "Story N" only | team |
| Branch protection | Not enabled yet. Settings in `docs/branch-protection.md`; needs the repo admin (c-madalin) | repo admin |
| Sample PDFs with a table and with multiple columns | Story 1 Task 1 needs both in `resources/`. The six current PDFs have small tables only (`compliance_responsibilities.pdf`, `updatecycle_checklist.pdf`) and none is multi-column (checked with pdftotext) | team |
| First full eval report and baseline | `eval/baseline.json` currently comes from a retrieval-only run (no chat model). Run `make eval` with `qwen2.5:7b` on a GPU machine, then `make eval-baseline`, so refusal and faithfulness are gated too | Story 4 |
| Numeric targets to agree with the product owner | Watcher latency (30 s proposed), ReDoS time limit, multi-query latency and token caps, similarity threshold, eval tolerance (0.02) | each story owner |
| Second embedding model for the re-index demo | `mxbai-embed-large` (1024 dims) is listed in `rag.store.models` as a candidate | Story 1 |
| Golden set size | 14 starter questions in `eval/data/golden-set.json`; Story 4 Task 4 needs at least 30 including unanswerable ones | Story 4 |
| Postman collection | Story 3 Task 1 requires one committed; no folder exists yet (suggest `docs/postman/`) | Story 3 |

## Stubs in the walking skeleton

`grep -rn "TODO(story-" --include=*.java .` lists them in code.

| Story / task | Stub today |
|---|---|
| 1.1 | Page-based PDF reading and `TokenTextSplitter` without overlap or structure awareness |
| 1.2 | No checksum skip. Replacement is delete-then-add, not atomic |
| 1.3 | Delete works; there's no orphan check |
| 1.4 | Manifest table and `/manifest` exist; no reconciliation script or re-index endpoint (the store API supports it) |
| 2.1 to 2.4 | ingest-dynamic endpoints return 501 |
| 3.1 | Any filter is rejected with 400 |
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
