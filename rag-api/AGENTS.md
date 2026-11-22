# rag-api (infra)

Port 8081. Owns `POST /api/ask`. `AskService` calls retrieval-pre `/retrieve`, then retrieval-post `/answer`,
records per-stage timings and maps stage failures (4xx passes through, anything else is 502). It holds no RAG logic;
put new behaviour in the stage that owns it. `AskControllerTest` mocks both stages with `MockRestServiceServer`.
