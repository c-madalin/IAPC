---
name: eval-regression
description: Run the IAPC evaluation harness (golden set, recall@k, MRR, refusal accuracy, citation faithfulness), read and compare reports, update the baseline, and fix a failing eval-check CI job. Use after changing pipeline code (ingestion, retrieval, answering, shared modules) or the golden set, when CI's eval-check fails, or when a task asks for metrics with and without a stage.
---

# Eval and the regression gate

## What CI checks (eval-check workflow)

`scripts/eval-check.py`:

1. `eval/reports/latest.json` exists and its `pipelineHash` equals `scripts/pipeline-hash.py` for the current code.
   The hash covers `src/main` of rag-common, rag-platform, rag-store, rag-ingest, rag-api, ingest-static, ingest-dynamic,
   retrieval-pre, retrieval-post, every `pom.xml` (dependency bumps change behaviour), the `resources/` corpus and
   `eval/data/golden-set.json`. Tests and docs don't count.
2. The report has no errored questions.
3. No metric in `recall_at_k`, `mrr`, `refusal_accuracy`, `citation_faithfulness` is more than 0.02 below
   `eval/baseline.json`.

## Running it

Needs the full stack with real models and data:

```bash
make up                      # or make dev / make up-obs; wait until all services are registered
curl -X POST localhost:8080/ingest-static/documents/sync   # if ./resources changed
git add -A                   # the hash only counts tracked files
make eval                    # full run, writes eval/reports/latest.json
make eval-check              # same check as CI
```

Retrieval only (no chat model, fast): `make eval-retrieval`. Stage comparisons for task reports:

```bash
make eval ARGS="--eval.expand=false --eval.out=eval/reports/no-expand.json"
make eval ARGS="--eval.expand=true  --eval.out=eval/reports/expand.json"
make eval ARGS="--eval.rerank=false --eval.out=eval/reports/no-rerank.json"
```

Only `eval/reports/latest.json` is checked by CI. Commit comparison reports you cite in a PR.

Use the default models (`qwen2.5:7b`, `nomic-embed-text`) for any report you commit, so numbers are comparable.
The report records `chatModel`.

## Reading a report

`metrics` has the summary. `results[]` has one entry per question: `retrievedDocIds` (distinct, in rank order),
`hit`, `reciprocalRank`, `refused`, `topSimilarity`, `citationsGrounded`, `answer`, `error`. For the Story 4 Task 3
threshold, compare `topSimilarity` of answerable versus unanswerable questions and pick a value that separates them.

## Moving the baseline

When a change improves the metrics, or when a drop is intended (new harder questions), promote the report:

```bash
make eval-baseline           # copies latest.json to eval/baseline.json
```

Commit it with a message saying why the baseline moved. A baseline drop needs the team's agreement in the PR.

## When eval-check fails

| Message | Do |
|---|---|
| report is for pipeline X, the code is Y | Re-run `make eval` on the current code and commit the report |
| questions errored | Look at `results[].error`; usually a service was down or not registered yet |
| REGRESSION | Find which questions lost their hit or started refusing; fix the code, or justify and move the baseline |
| report missing | Run `make eval` and commit `eval/reports/latest.json` |

Never edit `latest.json` or `baseline.json` by hand, and never fake the pipeline hash.
