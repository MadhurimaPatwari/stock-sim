# stock-sim

A learning project: a simulated stock trading platform in Java (Spring Boot 4.1, Java 25, PostgreSQL).

## Prerequisites

- Docker (Docker Desktop or Docker Engine with Compose v2)
- JDK 25
- Maven 3.9+ (or the one bundled with IntelliJ IDEA)

## Running

```bash
docker compose --profile app up --build
```
## What's running

| Service    | URL                                   | Notes                                   |
|------------|---------------------------------------|-----------------------------------------|
| App        | http://localhost:8080/actuator/health | Should return `{"status":"UP"}`         |
| Swagger UI | http://localhost:8080/swagger-ui.html | Try the REST API from the browser         |
| PostgreSQL | localhost:5432                        | db/user/password: `stocksim`            |
| Adminer    | http://localhost:8081                 | Web UI to browse the database           |

In Adminer, log in with system PostgreSQL, server `postgres`, and the credentials above.


## Database migrations
Schema changes live in `src/main/resources/db/migration` as `V<number>__<description>.sql`.
Flyway applies new ones automatically on startup.

Never edit a migration that has already run. Add a new one instead.