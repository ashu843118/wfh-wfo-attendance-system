# WFH/WFO Attendance Tracking App

A responsive web/PWA application to track employee Work From Home (WFH) and Work From Office (WFO) attendance using geo-fencing. Built as a **modular monolith** demonstrating Principal Engineer-level backend design, PostGIS geospatial queries, transactional outbox async processing, Redis caching/locking, role-based dashboards, and enterprise-style UI.

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Frontend | React, Vite, React Router, Axios, Recharts, PWA-ready |
| Backend | Java 17, Spring Boot 3.x, Spring Security (JWT), JPA, Flyway |
| Database | PostgreSQL 16 + PostGIS |
| Cache/Lock | Redis, Redisson |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Deployment | Docker Compose |

## Architecture Summary

Single Spring Boot application with domain packages: `auth`, `employee`, `team`, `attendance`, `geofence`, `dashboard`, `outlier`, `notification`, `office`, `policy`, `outbox`, `audit`, `common`, `config`.

Check-in returns immediately; WFO/WFH classification runs asynchronously via transactional outbox + PostGIS `ST_DWithin`.

See [docs/architecture.md](docs/architecture.md) for details.

## Project Structure

```
wfh-wfo-attendance-app/
├── backend/          # Spring Boot modular monolith
├── frontend/         # React PWA
├── docs/               # Architecture, API, assumptions, trade-offs
├── screenshots/        # UI screenshots (add after running app)
├── docker-compose.yml
└── README.md
```

## Quick Start (Evaluator Mode)

**Recommended:** Docker Compose starts everything including PostGIS and Redis. You do **not** need to install PostgreSQL, PostGIS, or Redis locally.

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator Health | http://localhost:8080/actuator/health |

## Manual Development Mode

Start infrastructure only:

```bash
docker compose up -d postgres redis
```

**Backend:**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker-local
```

On Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=docker-local"
```

**Frontend:**

```bash
cd frontend
npm install
cp .env.example .env.local   # or create .env.local manually
npm run dev
```

Frontend runs at http://localhost:3000 with `VITE_API_BASE_URL=http://localhost:8080`.

## Demo Users

| Email | Password | Role | Notes |
|-------|----------|------|-------|
| employee@demo.com | password | EMPLOYEE | Engineering team; today left open for live check-in |
| manager@demo.com | password | MANAGER | Engineering team manager |
| leader@demo.com | password | LEADERSHIP | Organization-wide dashboard |
| admin@demo.com | password | ADMIN | Employee management (~100 employees) |

All seeded employees use the password **`password`**.

## Demo Data (Hybrid Seed)

On first startup with `docker`, `docker-local`, or test profiles, the app seeds realistic demo data automatically. Production (`prod` profile) never seeds demo data.

| Item | Count |
|------|-------|
| Employees | 100 |
| Teams | 5 (Engineering, Quality Assurance, Product, HR, Finance) |
| Managers | 5 (one per team) |
| Leadership / Admin | 1 each |
| Office locations | 5 |
| Attendance policies | 1 per team |
| Attendance history | Last 30 calendar days (weekdays only) |
| Outliers | Intentional per-team outliers |
| Notifications | Manager outlier alerts + sample employee reminders |

**Seed strategy**

1. **CSV reference data** in `backend/src/main/resources/demo-data/`:
   - `teams.csv`, `employees.csv`, `office_locations.csv`, `attendance_policies.csv`
2. **Java deterministic generator** for attendance records, outliers, and notifications (fixed random seed `42`).

**Idempotency:** If the database already contains 90+ employees, seeding is skipped on restart.

**Attendance mix (approximate):** 55–65% WFO, 25–35% WFH, 5–10% absent, 8–12% late check-ins, 2–5% missing check-outs, plus a small number of classification-pending records for dashboard demos.

### Reset demo data

Remove Docker volumes and rebuild for a completely fresh seed:

```bash
docker compose down -v
docker compose up --build
```

This drops PostgreSQL data and re-runs Flyway migrations plus the Java demo seeder.

## Database Inspection (DBeaver)

PostGIS runs inside Docker and is exposed on **localhost:5433** to avoid conflicts with a local PostgreSQL install.

| Setting | Value |
|---------|-------|
| Host | localhost |
| Port | 5433 |
| Database | attendance_db |
| Username | attendance_user |
| Password | attendance_pass |

## Running Tests

**Backend:**

```bash
cd backend
./mvnw test
```

**Frontend build:**

```bash
cd frontend
npm run build
```

## Screenshots

Add screenshots of login page and dashboards to the `screenshots/` folder after running the app locally.

## Hosted Demo

_Placeholder for future Vercel/Netlify (frontend) + Render/Railway (backend) deployment._

Configure frontend with `VITE_API_BASE_URL` pointing to the hosted backend URL.

## Spring Profiles

| Profile | Use case |
|---------|----------|
| `docker-local` | Manual backend against Docker Postgres/Redis on localhost:5433 |
| `docker` | Full Docker Compose stack (service hostnames `postgres`, `redis`) |
| `prod` | Hosted deployment with env vars (`DATABASE_URL`, `JWT_SECRET`, etc.) |

The base `application.yaml` does not include a datasource — always activate a profile when running the backend.

## Geolocation Notes

- Check-in/check-out requires browser location permission.
- HTTPS (or localhost) is required for the Geolocation API in most browsers.
- Location is captured only on explicit check-in/check-out actions, not continuously.

## Known Limitations & Future Enhancements

- Demo JWT auth only (enterprise SSO in production)
- In-app notifications only (no email/SMS/push)
- Polling instead of WebSocket/SSE
- Async classification (eventually consistent dashboards)
- Kafka mentioned as future replacement for in-process outbox poller
- Module extraction to microservices when scale requires

See [docs/tradeoffs.md](docs/tradeoffs.md) and [docs/assumptions.md](docs/assumptions.md).

## Documentation

- [Architecture](docs/architecture.md)
- [API Design](docs/api-design.md)
- [Assumptions](docs/assumptions.md)
- [Trade-offs](docs/tradeoffs.md)
