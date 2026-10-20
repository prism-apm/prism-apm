FROM eclipse-temurin:21.0.12.1_1-jre-noble@sha256:0c324fbe2e1455c3159184717440f3b52906ef82a39c4a6efc9edb401d31971a

WORKDIR /app

ARG ARG_SERVICE_NAME=prism-apm-parser
ARG ARG_DIR_NAME=${ARG_SERVICE_NAME}

COPY ${ARG_DIR_NAME}/target/*.jar /app/app.jar

## show service name in terminal
ENV SERVICE_NAME=${ARG_SERVICE_NAME}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
