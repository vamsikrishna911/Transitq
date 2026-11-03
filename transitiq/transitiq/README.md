# TransitIQ – Smart Public Transportation and Route Analytics

Spring Boot 3 (Java 17+) + MySQL + vanilla HTML/CSS/JS + Leaflet/OpenStreetMap. One application serves both the API and the UI.
Algorithms (BFS, DFS, Dijkstra, Yen-style alternatives) are hand-written in `com.transitiq.algorithms`.

## Prerequisites
- JDK 17 or 21 (`java -version`)
- Maven 3.9+ (`mvn -v`)
- MySQL 8+ running locally (or any MySQL-compatible managed DB)
- Internet access in the browser for map tiles and Leaflet (CDN)

## Database setup
```sql
CREATE DATABASE transitiq CHARACTER SET utf8mb4;
```
Tables are created from `src/main/resources/schema.sql` on startup. If `bus_stops` is empty, `src/main/resources/sample-data.sql`
is loaded automatically (15 stops, 5 routes, 25 connections, deterministic distances/times/fares).

## Configuration (environment variables – no passwords in code)
| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/transitiq?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | *(empty)* |
| `PORT` | `8080` |

## Run locally
```bash
export DB_PASSWORD='your-mysql-password'      # Windows PowerShell: $env:DB_PASSWORD='...'
mvn spring-boot:run
```
Open http://localhost:8080 – health check: http://localhost:8080/api/health

## Tests
```bash
mvn test
```
`AlgorithmsTest` covers graph construction, BFS, DFS, Dijkstra (distance/time/fare), route reconstruction, alternatives,
disconnected networks and invalid input. Fixture values are documented at the top of the test file.

## Build the JAR
```bash
mvn clean package
java -jar target/transitiq.jar
```

## Try the API (Postman or curl)
```bash
curl localhost:8080/api/stops
curl "localhost:8080/api/route/find?source=1&destination=8&mode=TIME"      # DISTANCE | TIME | FARE | STOPS
curl "localhost:8080/api/route/alternatives?source=1&destination=8&mode=DISTANCE"
curl -X POST localhost:8080/api/stops -H 'Content-Type: application/json' \
     -d '{"name":"Test Stop","latitude":12.97,"longitude":77.6,"description":"demo"}'
curl -X POST localhost:8080/api/routes -H 'Content-Type: application/json' \
     -d '{"busCode":"R6","name":"Express","stopIds":[1,12,14]}'
curl localhost:8080/api/analytics ; curl localhost:8080/api/analytics/connectivity
```
Errors are JSON: `{"error":"..."}` (400 invalid input / same source+destination / unknown id, 404 missing stop, 409 constraint conflict).
A disconnected pair returns HTTP 200 with `"found": false` and an explanatory `message`.

## Deploy on Render
Render has no managed MySQL, so use an external MySQL-compatible service (Aiven, Railway, PlanetScale-compatible, TiDB Cloud, etc.).
1. Push this folder to a GitHub repository.
2. Render → **New → Web Service** → connect the repo → **Runtime: Docker** (uses the included `Dockerfile`).
3. Environment variables: `DB_URL` (JDBC form, e.g. `jdbc:mysql://HOST:PORT/DBNAME?useSSL=true`), `DB_USERNAME`, `DB_PASSWORD`. Render sets `PORT` itself.
4. Health Check Path: `/api/health`. Deploy; the demo data loads on first start.

## Common errors
| Error | Fix |
|---|---|
| `Communications link failure` / `Connection refused` | MySQL is not running or `DB_URL` host/port is wrong. |
| `Access denied for user` | Set `DB_USERNAME` / `DB_PASSWORD`. |
| `Unknown database 'transitiq'` | Run `CREATE DATABASE transitiq;` (or keep `createDatabaseIfNotExist=true` and grant create rights). |
| `Port 8080 already in use` | `PORT=9090 mvn spring-boot:run`. |
| Blank map tiles | Browser needs internet access to tile.openstreetmap.org. |
| `release version 17 not supported` | Use JDK 17+ and make `JAVA_HOME` point to it. |

## Limitations
- Analytics computes all-pairs shortest paths per request: fine for hundreds of stops, not for very large networks.
- No authentication (the optional `users` table was intentionally omitted).
- Editing a stop's coordinates does not recompute stored connection distances.
