FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/quarkus-app/ /app/
RUN mkdir -p /app/.fhir/packages /app/data \
 && useradd -u 10001 -r -m -d /home/appuser -s /usr/sbin/nologin appuser \
 && chown -R appuser:appuser /app /home/appuser
ENV HOME=/app
USER appuser
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/quarkus-run.jar"]

