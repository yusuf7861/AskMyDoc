###############################
# Stage 1 — Build Spring Boot
###############################
FROM eclipse-temurin:17-jdk as build
LABEL authors="yjamal"

WORKDIR /app

# Copy Maven/Gradle wrapper + config first
COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn

# Download dependencies (cache layer)
RUN ./mvnw dependency:go-offline

# Copy the full source
COPY src ./src

# Build the Spring Boot JAR
RUN ./mvnw clean package -DskipTests


###############################
# Stage 2 — Final Runtime Image
###############################
FROM ubuntu:latest
LABEL authors="yjamal"

# Ubuntu does not include Java or top; install only what’s needed
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    procps \
    && apt-get clean

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Entry point: uses your "top" idea for debugging + runs Spring Boot
# If you want ONLY Spring Boot, I’ll give that version too.
ENTRYPOINT ["java", "-jar", "app.jar"]
