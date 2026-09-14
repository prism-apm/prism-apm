FROM eclipse-temurin:21-jre

COPY prism-explorer/target/*.jar /app/prism-explorer.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "/app/prism-explorer.jar", "--server.port=8091"]