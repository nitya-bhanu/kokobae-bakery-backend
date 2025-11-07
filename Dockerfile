# --- Stage 1: Build the application ---
# Use an official Maven image that includes JDK 17
FROM maven:3.9.6-eclipse-temurin-17 as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project file and download dependencies first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application into a JAR file. Skip tests for faster builds.
RUN mvn package -DskipTests

# --- Stage 2: Create the final, lightweight runtime image ---
# Use a smaller base image with only the Java Runtime Environment (this part remains the same)
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Copy the executable JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
