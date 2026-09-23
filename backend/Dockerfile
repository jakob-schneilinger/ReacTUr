# --- Stage 1: Bauen (Kompilieren) der Applikation ---
# Wir nutzen ein Image, das Maven und Java 17 enthält
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Ausführen der Applikation ---
# HIER GEÄNDERT: Java 21 statt 17
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]