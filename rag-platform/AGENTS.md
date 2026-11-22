# rag-platform (infra)

Spring auto-configuration every service gets by depending on it:

- `rag-platform-defaults.properties`: actuator, OTLP export (off unless `OTEL_ENABLED=true`), Eureka client defaults.
  A service's `application.yaml` overrides any of them.
- `logback-spring.xml`: console pattern with `[app,traceId,spanId]` plus the OpenTelemetry appender for Loki.
- `application-hostdev.properties`: the `make dev` profile.
- `ServiceClients`: Eureka-aware `RestClient` per service id, with `rag.clients.urls.<id>` overrides. It adds the
  load-balancer interceptor to a private builder; never publish a `@LoadBalanced RestClient.Builder` bean (Eureka's
  own client would use it and fail to register).
- `ApiExceptionHandler`: problem responses and logging for errors.
- `StageTimer`: times a stage into `rag.stage.duration{stage=...}` and the response's `timingsMs`.
