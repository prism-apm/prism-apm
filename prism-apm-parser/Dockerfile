FROM eclipse-temurin:24.0.2_12-jre-noble@sha256:b416d02335e702b0403ff280de9475a3348e29382285969c9d4e17862ce632e7

WORKDIR /app

ARG ARG_SERVICE_NAME=prism-apm-parser
ARG ARG_DIR_NAME=${ARG_SERVICE_NAME}

COPY ${ARG_DIR_NAME}/target/*.jar /app/app.jar

## show service name in terminal
ENV SERVICE_NAME=${ARG_SERVICE_NAME}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
