# ── Stage 1: build ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first to cache dependencies
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy sources and build the jar
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ── Stage 2: runtime ───────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Render provides PORT; default to 8080 locally
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
