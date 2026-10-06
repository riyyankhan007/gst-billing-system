# Stage 1: Build application
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Cache Maven wrapper and dependencies
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Build application bundle
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal, secure production runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN mkdir -p uploads && chown -R appuser:appgroup /app

COPY --from=builder /app/target/gst-billing-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8081

ENV PORT=8081
CMD ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar --server.port=${PORT}"]