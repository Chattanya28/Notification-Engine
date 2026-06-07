# Stage 1: Build the Maven application with Java 24
FROM maven:3.9.9-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime JRE 24 container
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/notification-platform.jar app.jar
EXPOSE 8080

# Run with standard security parameters
ENTRYPOINT ["java", "-jar", "app.jar"]
