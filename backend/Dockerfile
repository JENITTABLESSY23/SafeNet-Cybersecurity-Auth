# ── Build stage ──────────────────────────────────────────────
# Compiles the Spring Boot app into a runnable jar. This stage's ~700MB of
# Maven/JDK tooling never ends up in the final image below.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy just the pom first so Docker can cache the dependency download step
# and skip re-downloading everything on every code change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ── Run stage ────────────────────────────────────────────────
# Small JRE-only image — no Maven, no JDK, just what's needed to run the jar.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Ship with the seeded demo database baked into the image, so the deployed
# app starts with working demo logins/patients immediately. Comment this
# line out if you'd rather start from a completely empty database.
COPY safenet.db safenet.db

# Cloud hosts (Render, etc.) inject PORT and expect the app to bind to it —
# application.properties already reads server.port=${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
