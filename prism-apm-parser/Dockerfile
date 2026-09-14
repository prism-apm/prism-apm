FROM eclipse-temurin:21-jre

ARG ARG_SERVICE_NAME=prism-apm-parser
ARG ARG_DIR_NAME=${ARG_SERVICE_NAME}

COPY ${ARG_DIR_NAME}/target/*.jar /app/app.jar

## show service name in terminal
ENV SERVICE_NAME=${ARG_SERVICE_NAME}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
