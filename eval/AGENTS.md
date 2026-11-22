# eval (Story 4 Task 4: evaluation harness)

Owner: `<story-4-owner>`. Package `ro.iapc.rag.eval`. A command-line Spring Boot app, not a service.

Runs every question in `eval/data/golden-set.json` through the running stack (via the gateway):

- `POST /retrieval-pre/retrieve` for recall@k and MRR over distinct `doc_id`s,
- `POST /api/ask` for refusal accuracy and citation faithfulness (every cited doc is among the chunks used).

Writes `eval/reports/latest.json` with the pipeline hash from `scripts/pipeline-hash.py`. CI compares it with
`eval/baseline.json`. How to run and when: `.claude/skills/eval-regression/SKILL.md`.

## Tasks

- [ ] At least 30 question/source pairs including unanswerable ones (14 now). Story 4 Task 3 needs 10 out-of-scope
  questions.
- [ ] recall@k, MRR and a faithfulness check. The current faithfulness check only verifies citations point at
  retrieved chunks. A stronger check (LLM judge on each claim, or sentence-level overlap) is the TODO.
- [ ] One command (`make eval`) and a committed baseline report.
- [ ] Regression run required before merging pipeline changes (CI's eval-check enforces it).

## Code

- `EvalRunner`: the loop and the summary metrics. `Metrics`: hit@k, reciprocal rank.
- `EvalProperties`: `--eval.*` flags, including `--eval.expand`, `--eval.rerank`, `--eval.stitch`,
  `--eval.retrieval-only`.
