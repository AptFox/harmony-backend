# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Download Gradle dependencies to leverage Docker caching
RUN ./gradlew dependencies --no-daemon

# Copy the source code
COPY src ./src

# Build the Spring Boot application
RUN ./gradlew bootJar --no-daemon

# Stage 2: Create the production image
FROM eclipse-temurin:21-jre-jammy

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/harmony-backend-*.jar app.jar
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Define the entrypoint for running the application
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=25", "-XX:MaxRAMPercentage=60", "-Xss512k", "-XX:MaxJavaStackTraceDepth=20", "-XX:+UseContainerSupport", "-jar", "app.jar"]
