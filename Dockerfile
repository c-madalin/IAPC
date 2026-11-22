# One image per service: docker build --build-arg MODULE=retrieval-pre .
# The build stage doesn't depend on MODULE, so BuildKit builds the reactor once and every service reuses it.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY rag-common/pom.xml rag-common/
COPY rag-platform/pom.xml rag-platform/
COPY rag-store/pom.xml rag-store/
COPY rag-ingest/pom.xml rag-ingest/
COPY rag-test-support/pom.xml rag-test-support/
COPY eureka-server/pom.xml eureka-server/
COPY api-gateway/pom.xml api-gateway/
COPY rag-api/pom.xml rag-api/
COPY ingest-static/pom.xml ingest-static/
COPY ingest-dynamic/pom.xml ingest-dynamic/
COPY retrieval-pre/pom.xml retrieval-pre/
COPY retrieval-post/pom.xml retrieval-post/
COPY eval/pom.xml eval/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline -DexcludeReactor=true || true
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true

FROM eclipse-temurin:21-jre
ARG MODULE
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-*.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
