# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

# Download deps separately so layer is cached unless pom.xml changes
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline -q 2>/dev/null || true

COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Add a non-root user
RUN addgroup -S bidflare && adduser -S bidflare -G bidflare
USER bidflare

COPY --from=builder /build/target/bidflare-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
