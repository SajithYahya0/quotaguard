# Build stage: compile inside Docker, so the only prerequisite is Docker itself.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Dependencies first: this layer stays cached until pom.xml changes.
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Tests need Redis and Postgres; CI runs them against real services.
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: JRE only, no build tools.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
