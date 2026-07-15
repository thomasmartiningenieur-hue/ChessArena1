# --- Étape de build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# --- Étape finale ---
FROM eclipse-temurin:17-jre-jammy

# Installation de Stockfish depuis les dépôts Debian/Ubuntu (binaire Linux natif)
RUN apt-get update && \
    apt-get install -y --no-install-recommends stockfish && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV STOCKFISH_PATH=/usr/games/stockfish
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
