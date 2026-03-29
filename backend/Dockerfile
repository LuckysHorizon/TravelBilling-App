# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw mvnw.cmd ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests clean package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /app
ENV SERVER_PORT=8080

RUN groupadd -g 1001 spring && useradd -u 1001 -g spring -m spring

COPY --from=builder /app/target/*.jar app.jar

USER spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
