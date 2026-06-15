# WFH/WFO Attendance Tracking App

A responsive web/PWA application that tracks employee **Work From Office (WFO)** and **Work From Home (WFH)** attendance using geo-fencing. The system supports **Employee**, **Manager**, **Leadership**, and **Admin** roles with role-specific dashboards, daily attendance summaries, session/event history, and rule-based outlier detection.

Built as a **modular monolith** (Spring Boot + React) with PostgreSQL/PostGIS, Redis caching, transactional outbox processing, and Docker Compose for one-command local evaluation.

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 3.x, Spring Security (JWT), JPA, Flyway |
| Database | PostgreSQL 16 + PostGIS |
| Cache / locks | Redis, Redisson |
| Frontend | React, Vite, React Router, Axios, Recharts, PWA-ready |
| API docs | Springdoc OpenAPI / Swagger UI |
| Deployment | Docker Compose |

## Quick Start

**Recommended:** run the full stack with Docker Compose (PostgreSQL, PostGIS, Redis, backend, and frontend).

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

See [docs/local-setup.md](docs/local-setup.md) for database access, reset instructions, and local geolocation testing tips.

## Demo Users

All seeded users share the password **`password`**.

| Email | Role | Notes |
|-------|------|-------|
| employee@demo.com | EMPLOYEE | Engineering; today is left open for live attendance testing |
| manager@demo.com | MANAGER | Engineering team manager dashboard |
| leader@demo.com | LEADERSHIP | Organization-wide trends |
| admin@demo.com | ADMIN | Employee, office, and policy administration |

Additional demo employees (~100 total) are seeded across five teams. See demo CSVs in `backend/src/main/resources/demo-data/`.

## Final Attendance Behavior (MVP)

This is the **implemented product flow** evaluators should expect.

### On login / Employee Dashboard open

1. The app **automatically requests browser location permission** (no toggle to enable auto attendance).
2. While the app is open, location signals are sent periodically for auto attendance.
3. If permission is denied, a fallback message is shown and **manual check-in/check-out** remains available.

### Inside assigned office geofence

- If the employee has **no active session**, the system waits for a short stability period (~15 seconds inside the fence), then **auto check-in** is recorded.
- Session mode = **WFO**, check-in source = **AUTO**.
- **Auto checkout** is allowed for these sessions when the employee remains outside the assigned office geofence for a configured grace period (60 seconds in demo; 15–30 minutes recommended in production).

### Outside assigned office geofence

- The system **does not auto check-in** and **does not silently mark WFH**.
- A prompt asks: *"Do you want to check in as Work From Home?"*
  - **Check in as WFH** → manual/confirmed WFH session
  - **Not now** → no attendance created; manual check-in remains available

### Manual check-in / check-out

- **Manual check-in** always available when no active session exists.
- Backend classifies the session from current location vs assigned office geofence:
  - Inside fence → **WFO**
  - Outside fence → **WFH**
- **Manual checkout** is available whenever an active session is open.
- Manual and WFH-confirmed sessions **do not auto checkout**; they require manual checkout or **end-of-day (EOD) system close**.

### Same-day re-check-in

- Multiple sessions per day are supported.
- After checkout, check-in is enabled again.
- Check-in is disabled **only while a session is open**.
- Checkout is enabled **only while a session is open**.

### Daily summary and final mode

- **`attendance_events`** and **`attendance_sessions`** store detailed history (auto, manual, geofence, system events).
- **`attendance_records`** stores one **daily summary** row per employee per date.
- Final daily **`attendance_mode`** is **WFO** or **WFH only** — **no HYBRID** in MVP.
- Final mode is based on **`total_office_minutes`** (sum of all completed WFO session durations) compared to **`required_wfo_minutes`** (default **180**, configurable per team policy).
- A later WFH session on the same day does **not** downgrade a day that already met the office-time threshold.

Dashboards (Employee, Manager, Leadership) read from **`attendance_records`** daily summaries; drill-down APIs expose session/event history.

## Key Product Decisions

| Decision | Why |
|----------|-----|
| **One assigned office per employee (MVP)** | Simplifies geofence rules and cache keys; multi-office support is a documented future extension. |
| **Auto WFO inside fence, confirmed WFH outside** | Geofence presence is strong evidence of WFO; outside the fence could mean home, travel, leave, or GPS error — WFH requires explicit confirmation. |
| **Auto checkout only for auto WFO sessions** | Predictable behavior; manual/WFH sessions need explicit checkout or EOD close. |
| **WFO/WFH daily mode only (no HYBRID)** | Keeps manager and leadership reporting simple for MVP. |
| **Office minutes drive final WFO day** | Supports split days (office morning + home afternoon) while still counting as a WFO day when threshold is met. |
| **Location only while app is open** | Respects browser/PWA constraints and privacy expectations; no background tracking when the tab is closed. |
| **Redis office cache** | Reduces repeated DB reads for geofence validation; PostgreSQL remains source of truth. |
| **Transactional outbox (not Kafka)** | Simpler MVP infrastructure; Kafka noted as a future scale path. |

## Privacy

- Location is used **only for attendance classification** (check-in/out and auto attendance while the app is open).
- Location tracking is **active only while the Employee Dashboard PWA is open** in the browser.
- **No background location tracking** when the browser tab or PWA is closed.

## Project Structure

```
wfh-wfo-attendance-app/
├── backend/          # Spring Boot modular monolith
├── frontend/         # React PWA
├── docs/             # Architecture, API, assumptions, trade-offs, local setup
├── screenshots/      # UI screenshots
├── docker-compose.yml
└── README.md
```

## Demo Data

On first startup in `docker` / `docker-local` profiles, the app seeds realistic demo data (skipped if 90+ employees already exist).

| Item | Count |
|------|-------|
| Employees | ~100 |
| Teams | 5 |
| Office locations | 5 (Pune, Mumbai, Bangalore, Hyderabad, Delhi NCR) |
| Attendance policies | 1 per team |
| History | ~30 weekdays of attendance, sessions, outliers, notifications |

Reset completely:

```bash
docker compose down -v
docker compose up --build
```

## Running Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`.

## Documentation

- [Architecture](docs/architecture.md)
- [API Design](docs/api-design.md)
- [Assumptions](docs/assumptions.md)
- [Trade-offs](docs/tradeoffs.md)
- [Local Setup](docs/local-setup.md)

## Known Limitations & Future Enhancements

- Demo JWT auth (enterprise SSO in production)
- In-app notifications only (no email/SMS/push)
- Polling instead of WebSocket/SSE for live dashboard updates
- PWA foreground location only (native app for true background geofencing)
- Transactional outbox poller (Kafka as future event bus)
- Multiple offices per employee (future `employee_office_assignments` table)

See [docs/tradeoffs.md](docs/tradeoffs.md) and [docs/assumptions.md](docs/assumptions.md) for details.
