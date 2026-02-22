# Multi-stage build for SQLens — SQL Query Structure Visualizer

# Stage 1: Build application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy all pom.xml files first to cache Maven dependencies
COPY pom.xml .
COPY frontend/pom.xml frontend/
COPY backend/pom.xml backend/
RUN mvn dependency:go-offline -B

# Copy full source and build
# frontend-maven-plugin downloads Node v20 + runs npm ci + ng build
# spring-boot-maven-plugin packages Angular dist + backend into a fat JAR
COPY frontend/ frontend/
COPY backend/ backend/
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built fat JAR from build stage
COPY --from=build /build/backend/target/sqlens-backend-*.jar app.jar

# Expose port (matches server.port in application.properties)
EXPOSE 8022

# Set JVM options and override default port for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8022

# Health check via Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8022/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
