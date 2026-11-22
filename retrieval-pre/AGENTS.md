# retrieval-pre (Story 3: pre-retrieval)

Owner: `<story-3-owner>`. Port 8084. Package `ro.iapc.rag.retrieval.pre`.

Turns a raw question and URL filters into ranked candidate chunks. Also serves neighbor chunks for retrieval-post.

## Code

- `RetrievalService.retrieve`: skeleton trims the query and runs a similarity search with `topN` (default 20).
  Rejects every filter with 400.
- `RetrievalService.neighbors` and `GET /chunks/neighbors`.
- `RetrievalProperties`: `rag.retrieval.*`.
- Filters must become a Spring AI `Filter.Expression` (built with `FilterExpressionBuilder`), never a string from
  the request.

## Tasks (full criteria in docs/epic-1-tasks.md)

- [ ] 3.1 Parse `rawFilters` into a validated filter object; whitelisted fields and operators; 400 listing the valid
  fields; AND/OR; committed Postman collection (suggest `docs/postman/`); no filter falls through to the normal flow.
- [ ] 3.2 Ordered, precompiled, idempotent regex rules; ReDoS test under a time limit; polite prefixes and trailing
  question marks stripped (table of at least 15 cases); original query logged.
- [ ] 3.3 Acronym dictionary in an external editable file (for example `src/main/resources/acronyms.yml`, overridable
  by a file path); search uses acronym and full form; word boundaries; zero matches pass through.
- [ ] 3.4 Configurable number of variants with result fusion (RRF); latency and token cost logged with a cap;
  fallback to the original query on failure or timeout; recall@k with and without (use `stages.expand` and
  `make eval ARGS="--eval.expand=false"`).

Story 2 Task 2 needs a default `is_latest == true` filter; agree with the Story 2 owner on how it's applied.

## Tests

`RetrievalControllerIT` shows HTTP plus database. Normalization, acronyms and filter parsing are pure logic: test
them as plain JUnit tests, parameterised with `@ParameterizedTest` and `@CsvSource` for the 15-case table.
