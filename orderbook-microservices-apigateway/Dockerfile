# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies first (better layer caching)
COPY pom.xml .
COPY src ./src

# Build skipping tests (or run them if you want strict CI)
RUN mvn clean package -DskipTests

# Stage 2: Runtime - small JRE image
FROM eclipse-temurin:21-jre-alpine

# create  a non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# copy only the final jar for space efficiency
COPY --from=builder /build/target/*.jar app.jar

# change ownership
RUN chown -R spring:spring /app

# run as non-root user
USER spring:spring

# expose spring cloud gateway port
EXPOSE 4999

# Optimized JVM flags for WebFlux/gateway
ENV JAVA_OPTS="\
    -XX:InitialRAMPercentage=75.0 \
    -XX:MaxRAMPercentage=80.0 \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    --enable-preview"

# Start the application
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar