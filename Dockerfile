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
FROM eclipse-temurin:17-jre-alpine
LABEL authors="yjamal"

WORKDIR /app

# Create upload directory (mount a volume here in production for persistence)
RUN mkdir -p /app/uploads

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
