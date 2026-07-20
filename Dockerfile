# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src ./src

# Download deps separately so the layer is cached unless pom.xml changes.
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline -q


RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Add a non-root user
RUN addgroup -S bidly && adduser -S bidly -G bidly
USER bidly

COPY --from=builder /build/target/bidly-*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
