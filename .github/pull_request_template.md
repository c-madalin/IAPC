## What and why

<!-- One or two sentences. Link the task, e.g. "Story 3 Task 2: query normalization". -->

## How it was tested

<!-- Commands you ran and what you checked by hand. -->

## Definition of done

The professor's baseline, plus the task's own criteria in docs/epic-1-tasks.md. Tick only what's true.

- [ ] The task's acceptance criteria are met (list any that aren't, and why)
- [ ] Tests: at least one success path and one failure path for the new behaviour
- [ ] `make verify` passes locally (formatting, unit and integration tests)
- [ ] Pipeline code, a pom.xml, resources/ or the golden set changed? Then `make eval` was run and `eval/reports/latest.json` is committed
- [ ] Errors and key events are logged
- [ ] No secrets; new configuration is in `.env.example` and docs/configuration.md
- [ ] README, module AGENTS.md or docs updated
- [ ] Ready to demo to the product owner
