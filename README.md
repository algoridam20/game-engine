# Game Engine

A modern, multi-module Spring Boot project for managing game services, players, and leaderboards.

## Prerequisites
- **Java 21** (LTS) is required for this project.
- **Maven 3.9+**
- **MySQL 8.0+**
- **Redis** (Optional - currently not enabled)

## Project Modules
- `game-commons`: Shared configurations (Caffeine cache) and DTOs.
- `game-player-service`: User management with JPA and Spring Security.
- `game-service`: Core game management logic and APIs.
- `game-road-not-taken`: Specific implementation of the "Road Not Taken" game.
- `game-leaderboard-service`: Leaderboard tracking and scoring.
- `game-server`: Main executable application and central configuration.

## Getting Started

### 1. Build the Project
Ensure you are using Java 21. If your default JDK is different, set `JAVA_HOME` first:

```bash
# MacOS example (using Homebrew)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean install -DskipTests
```

### 2. Configure the Database
The application is configured to connect to MySQL at `localhost:3306`. Ensure you have a database named `game_db`:

```sql
CREATE DATABASE game_db;
```

Update `game-server/src/main/resources/application.yml` with your database credentials if they differ from the defaults (root/password).

### 3. Start the Service
Build all modules first so `game-player-service` is on the runtime classpath, then run the server:

```bash
mvn clean install -DskipTests
mvn spring-boot:run -pl game-server
```

If you changed code in a library module (for example `game-player-service`), run `mvn install` again before restarting. Running `mvn spring-boot:run -pl game-server` alone can leave Spring Security on the default configuration and return `401` on public passkey endpoints.
### 4. Generate jwt secret

```bash
openssl rand -base64 64 | tr -d '\n'
```

### 5. Verify Installation
Once started, the health check endpoint will be available at:
`http://localhost:8080/api/v1/health`

Documentation and API testing are available via Swagger:
`http://localhost:8080/swagger-ui.html`

## Documentation
Additional documentation can be found in the `game-docs` folder.
