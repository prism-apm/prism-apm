FROM eclipse-temurin:24.0.2_12-jre-noble@sha256:b416d02335e702b0403ff280de9475a3348e29382285969c9d4e17862ce632e7

WORKDIR /app

ARG OTEL_JAVA_AGENT_VERSION=2.25.0
# Download the JAR file and its checksum file to vefity the file integrity.
RUN set -eux; \
    mkdir -p /app/javaagent; \
    curl -fSL --retry 3 --retry-delay 2 \
      -o /app/javaagent/opentelemetry-javaagent.jar \
      "https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent-${OTEL_JAVA_AGENT_VERSION}.jar"; \
    curl -fSL --retry 3 --retry-delay 2 \
      -o /app/javaagent/opentelemetry-javaagent.jar.sha256 \
      "https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent-${OTEL_JAVA_AGENT_VERSION}.jar.sha256"; \
    test -s /app/javaagent/opentelemetry-javaagent.jar; \
    printf '%s  %s\n' "$(cat /app/javaagent/opentelemetry-javaagent.jar.sha256)" /app/javaagent/opentelemetry-javaagent.jar | sha256sum -c -

ARG ARG_SERVICE_NAME=prism-apm-ui-server
ARG ARG_DIR_NAME=${ARG_SERVICE_NAME}

COPY ${ARG_DIR_NAME}/target/*.jar /app/app.jar

ENV SERVICE_NAME=${ARG_SERVICE_NAME} \
    OTEL_SERVICE_NAME="${ARG_SERVICE_NAME}" \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://host.docker.internal:4318" \
    OTEL_METRICS_EXPORTER=none \
    OTEL_LOGS_EXPORTER=none \
    OTEL_SEMCONV_STABILITY_OPT_IN=http,database,messaging,rpc

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/javaagent/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
