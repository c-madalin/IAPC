---
name: definition-of-done
description: Checklist to walk before opening or approving a pull request in the IAPC repo, combining the professor's baseline with the task's own acceptance criteria. Use when finishing a task, preparing a PR, writing the PR description, or reviewing someone else's PR.
---

# Definition of done

A task is done only when its own criteria (docs/epic-1-tasks.md and the module's AGENTS.md checklist) and the
baseline below are all met. Walk through every item; for each one either show the evidence or say plainly that it
isn't met and why. Don't tick boxes you haven't checked.

## 1. Task criteria

- Open docs/epic-1-tasks.md and copy the task's acceptance criteria into the PR description.
- For each criterion: name the test, command output or screenshot that proves it.
- Numeric targets (30 s, time limits, thresholds) are proposals to agree with the product owner; if one is still
  open, it's in docs/open-items.md. Say which value you used.

## 2. Tests

- At least one success-path and one failure-path test for the new behaviour. Name them in the PR.
- `make verify` passes (formatting, unit, integration). Paste the summary line.
- No test calls real Ollama or the internet.

## 3. Eval

- Did main code change in rag-common, rag-platform, rag-store, rag-api, ingest-*, retrieval-*, or did a
  `pom.xml`, `resources/` or eval/data/golden-set.json change? Then run the eval-regression skill: `make eval`, commit `eval/reports/latest.json`,
  and report the metric deltas in the PR. CI fails otherwise.

## 4. Operability

- Errors and key events are logged (ingested, skipped, failed, refused, fell back), with ids and counts, without
  document text or secrets.
- Stage timings use `StageTimer`; new metrics use names in `MetricNames`.
- New config: `application.yaml` placeholder, `.env.example` if user-facing, `compose.yaml` if the container needs
  it, a row in docs/configuration.md.
- No secrets in the diff (gitleaks runs in CI, but check yourself: `git diff main --stat`, then read the diff).

## 5. Docs

- The module's AGENTS.md checklist is ticked for what this PR finishes, and its "Code" section still matches.
- Contracts changed? docs/contracts.md and every caller updated in the same PR.
- Anything left undecided goes into docs/open-items.md, not into a comment.
- PR text and docs are plain and specific; run the unslop patterns over them.

## 6. Scope and review

- The diff stays in the task's module. Shared-module or infra changes are separate commits and called out.
- Formatting only via `make fmt`, no unrelated reformatting.
- A teammate reviews; the author doesn't merge their own PR without an approval. Squash merge.
- Before demo: the behaviour works through the gateway on `make up`, not only in tests.

## PR description template

The repository's PR template (.github/pull_request_template.md) has the short checklist; fill in "What and why",
"How it was tested" with real commands and results, and the per-criterion evidence from step 1.
