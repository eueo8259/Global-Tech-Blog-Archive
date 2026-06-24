FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY app.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
