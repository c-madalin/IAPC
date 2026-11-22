---
name: spring-ai-2-reference
description: Verified Spring AI 2.0.3, Spring Boot 4.0.10 and Spring Cloud 2025.1.5 API notes for this repo (vector store, Ollama chat and embeddings, documents, splitters, PDF readers, filters, Jackson 3, Boot 4 starters and test slices). Use before writing or changing code that touches Spring AI, Boot auto-configuration, tests or gateway config, because Spring AI 1.x and milestone APIs from training data differ.
---

# Spring AI 2.0 / Boot 4 reference for this repo

Everything here was checked against the 2.0.3 / 4.0.10 jars. If you need an API not listed, read the source instead
of guessing:

```bash
# Sources jar for any Spring AI module, unpacked into a scratch directory
curl -sO https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-<module>/2.0.3/spring-ai-<module>-2.0.3-sources.jar
unzip -q spring-ai-<module>-2.0.3-sources.jar -d spring-ai-<module>
# Or list a class's methods from the local repository
javap -cp ~/.m2/repository/org/springframework/ai/spring-ai-<module>/2.0.3/spring-ai-<module>-2.0.3.jar <fqcn>
```

## Artifacts we use

`spring-ai-pgvector-store`, `spring-ai-ollama`, `spring-ai-client-chat`, `spring-ai-pdf-document-reader`,
`spring-ai-model`. We don't use the `spring-ai-starter-*` auto-configuration; beans are built in code
(`rag-store`, `retrieval-post/ChatModelConfiguration`). Also in the BOM: `spring-ai-jsoup-document-reader`,
`spring-ai-tika-document-reader`, `spring-ai-rag`.

## Documents and splitting

```java
Document doc = Document.builder().id(id).text(text).metadata(Map.of(MetadataKeys.DOC_ID, docId)).score(0.8).build();
doc.getText(); doc.getMetadata(); doc.getScore(); doc.getId();

List<Document> pages = new PagePdfDocumentReader(resource).read();          // metadata "page_number", "file_name"
new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()...build());
new ParagraphPdfDocumentReader(resource, config).read();                    // needs a PDF outline (TOC)

TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withChunkSize(400).withMinChunkSizeChars(350).withMinChunkLengthToEmbed(5)
        .withMaxNumChunks(10000).withKeepSeparator(true).build();          // no overlap option
List<Document> chunks = splitter.apply(pages);
```

`TokenTextSplitter` has no overlap; Story 1 Task 1 needs its own splitter or a wrapper.

## Vector store

Get stores from `VectorStoreProvider` (rag-store), not by building `PgVectorStore` yourself.

```java
VectorStore store = stores.active();
store.add(documents);                                   // embeds, then upserts by id
store.delete(List.of(id1, id2));
store.delete(new FilterExpressionBuilder().eq(MetadataKeys.DOC_ID, docId).build());
List<Document> hits = store.similaritySearch(SearchRequest.builder()
        .query(q).topK(20)
        .similarityThreshold(0.3)                      // or .similarityThresholdAll() for 0.0
        .filterExpression(expression)                  // Filter.Expression; there is also a String overload, don't use it with user input
        .build());
```

Scores are similarities in [0, 1]. With threshold 0 the SQL is `distance < 1`, so a vector orthogonal to the query
(similarity exactly 0) is excluded.

`FilterExpressionBuilder b = new FilterExpressionBuilder();` gives `b.eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `in`,
`nin`, `and`, `or`, `not`, `group`. Call `.build()` on the final `Op` to get a `Filter.Expression`.

Table layout created by PgVectorStore: `id uuid primary key default uuid_generate_v4(), content text, metadata json,
embedding vector(N)`, HNSW index `<table>_index` with `vector_cosine_ops`. Metadata is `json`, so query it as
`metadata->>'key'` and cast (`(metadata->>'chunk_index')::int`).

## Ollama

```java
OllamaApi api = OllamaApi.builder().baseUrl(url).build();
OllamaEmbeddingModel embed = OllamaEmbeddingModel.builder().ollamaApi(api)
        .options(OllamaEmbeddingOptions.builder().model("nomic-embed-text").build()).build();
OllamaChatModel chat = OllamaChatModel.builder().ollamaApi(api)
        .options(OllamaChatOptions.builder().model("qwen2.5:7b").temperature(0.0).build()).build();
```

`OllamaChatOptions.builder().disableThinking()` exists for thinking models (qwen3). Models are not pulled
automatically (`PullModelStrategy.NEVER` by default); `make models` pulls them.

## Chat

```java
ChatResponse r = chatModel.call(new Prompt(List.of(new SystemMessage(system), new UserMessage(user))));
String text = r.getResult().getOutput().getText();
Usage usage = r.getMetadata().getUsage();              // getPromptTokens(), getCompletionTokens()
```

`ChatClient` (spring-ai-client-chat) also works: `ChatClient.builder(chatModel).build().prompt().system(..).user(..).call().content()`.

## Boot 4 and Spring Cloud 2025.1

- Starters: `spring-boot-starter-webmvc`, `-restclient`, `-flyway`, `-opentelemetry`; test starters
  `spring-boot-starter-webmvc-test` etc.
- Test imports: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` and `.WebMvcTest`;
  `org.springframework.boot.test.web.server.LocalServerPort`; `MockMvcTester` from
  `org.springframework.test.web.servlet.assertj`.
- Testcontainers 2: `org.testcontainers.postgresql.PostgreSQLContainer` (not generic), artifact
  `testcontainers-postgresql`.
- Jackson 3: `tools.jackson.databind.json.JsonMapper`, `tools.jackson.core.type.TypeReference`. Exceptions are
  unchecked (`JacksonException`). Annotations are still `com.fasterxml.jackson.annotation.*`.
- `DataSourceAutoConfiguration` moved to `org.springframework.boot.jdbc.autoconfigure`; `RestClientAutoConfiguration`
  and `RestClientBuilderConfigurer` to `org.springframework.boot.restclient.autoconfigure`.
- HTTP client timeouts: `spring.http.clients.connect-timeout`, `spring.http.clients.read-timeout`.
- OTLP: `management.opentelemetry.tracing.export.otlp.endpoint`, `management.opentelemetry.logging.export.otlp.endpoint`,
  `management.otlp.metrics.export.url`; enable flags `management.tracing.export.otlp.enabled`,
  `management.logging.export.otlp.enabled`, `management.otlp.metrics.export.enabled`.
- Gateway: `spring.cloud.gateway.server.webflux.routes`, `...discovery.locator.enabled`.
- Load balancing: Eureka's HTTP client uses any `RestClient.Builder` bean, so never declare a `@LoadBalanced`
  builder bean. Use `ServiceClients` (rag-platform).
