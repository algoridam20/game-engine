FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /src

COPY pom.xml .
COPY game-commons/pom.xml game-commons/pom.xml
COPY game-player-service/pom.xml game-player-service/pom.xml
COPY game-service/pom.xml game-service/pom.xml
COPY game-seven-eight/pom.xml game-seven-eight/pom.xml
COPY game-road-not-taken/pom.xml game-road-not-taken/pom.xml
COPY game-leaderboard-service/pom.xml game-leaderboard-service/pom.xml
COPY game-server/pom.xml game-server/pom.xml

COPY game-commons game-commons
COPY game-player-service game-player-service
COPY game-service game-service
COPY game-seven-eight game-seven-eight
COPY game-road-not-taken game-road-not-taken
COPY game-leaderboard-service game-leaderboard-service
COPY game-server game-server

RUN mvn -pl game-server -am package -DskipTests -Dfmt.skip=true

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /src/game-server/target/game-server-0.0.1-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

ENV PORT=8080

CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
