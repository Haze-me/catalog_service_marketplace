# ── Stage 1: Builder ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first for layer caching — dependencies only re-download when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Final (Distroless) ───────────────────────────────────────────────
# gcr.io/distroless/java21-debian12 has the JRE but no shell
FROM gcr.io/distroless/java21-debian12

WORKDIR /app

# Copy only the built JAR from the builder stage
# Adjust the JAR filename to match what Maven produces (check target/ folder)
COPY --from=builder /app/target/catalog-service-*.jar app.jar

EXPOSE 8081

# Distroless uses exec form — no shell wrapper needed
CMD ["app.jar"]