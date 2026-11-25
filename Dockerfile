# Stage 1: Build application
FROM maven:3.9.16-eclipse-temurin-21 AS builder

WORKDIR /app

# Use Google's Maven Central mirror to avoid HTTP 429 rate-limiting on shared CI/cloud builders
RUN mkdir -p /root/.m2 && printf '%s\n' \
  '<settings>' \
  '  <mirrors>' \
  '    <mirror>' \
  '      <id>google-maven-central</id>' \
  '      <name>GCS Maven Central mirror</name>' \
  '      <url>https://maven-central.storage-download.googleapis.com/maven2</url>' \
  '      <mirrorOf>central</mirrorOf>' \
  '    </mirror>' \
  '  </mirrors>' \
  '</settings>' > /root/.m2/settings.xml

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:resolve -B

# Build application bundle
COPY src ./src
RUN mvn clean package -DskipTests -B

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