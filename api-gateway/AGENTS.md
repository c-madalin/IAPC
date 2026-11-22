# api-gateway (infra)

Port 8080, the only public port. Spring Cloud Gateway (WebFlux, not MVC, so it doesn't use rag-platform). Routes:
`/api/**` to rag-api and, through the Eureka discovery locator, `/<service-id>/**` to every registered service with
the prefix stripped. Properties are under `spring.cloud.gateway.server.webflux.*`.
