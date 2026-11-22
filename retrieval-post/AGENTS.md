# retrieval-post (Story 4: post-retrieval and grounding)

Owner: `<story-4-owner>`. Port 8085. Package `ro.iapc.rag.retrieval.post`.

Takes the question and retrieval-pre's candidates, picks and expands the best ones, and produces a grounded,
cited answer or "I don't know".

## Code

- `AnswerService`: skeleton takes the first `max-context-chunks` chunks, builds `CONTEXT [source, version, page]`
  blocks and calls the chat model. Refuses without a model call when there are no chunks. Emits
  `rag.refusals` and `rag.llm.tokens`.
- `ChatModelConfiguration`: Ollama chat model from `rag.answer.*`.
- Neighbors: call retrieval-pre with `ServiceClients.forService(ServiceNames.RETRIEVAL_PRE)` and
  `GET /chunks/neighbors`.

## Tasks (full criteria in docs/epic-1-tasks.md)

- [ ] 4.1 Cross-encoder reranker: wide top-N (20) to top-k (5); measured MRR or recall improvement on the golden set
  (`--eval.rerank=false` vs `true`); pinned model version and documented latency; on failure keep the original order.
- [ ] 4.2 Merge adjacent chunks by `chunk_index`, dedupe overlap, token budget, never cross documents; tests for the
  first chunk, last chunk and a single-chunk document.
- [ ] 4.3 Header on every block; prompt says answer only from context and cite; below the similarity threshold answer
  "I don't know"; threshold justified with eval data (`topSimilarity` is in every eval result); all 10 out-of-scope
  questions refused.

The eval harness (Task 4.4) lives in the `eval` module; see its AGENTS.md.

Ollama has no reranking API. Options: run a cross-encoder through ONNX Runtime or DJL in this service, or a small
sidecar container. Pin the model version either way. Decide early, it affects the image size.

## Tests

`AnswerControllerTest` with `FakeChatModel` shows the pattern. Assert on `chatModel.lastPrompt()` to check what the
model was given.
