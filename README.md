# WFH/WFO Attendance Tracking System

A responsive web/PWA application that tracks employee **Work From Office (WFO)** and **Work From Home (WFH)** attendance using geo-fencing. The system supports **Employee**, **Manager**, **Leadership**, and **Admin** roles with role-specific dashboards, daily attendance summaries, session/event history, and rule-based outlier detection.

Built as a **modular monolith** (Spring Boot + React) with PostgreSQL/PostGIS, Redis caching, transactional outbox processing, and Docker Compose for one-command local evaluation.

---

## Project Overview

The WFH/WFO Attendance Tracking System validates employee presence against an **assigned office geofence** to classify attendance. In MVP, each employee has **one assigned office**. Location is used purposefully:

- **Before check-in:** one browser location read decides auto WFO vs WFH confirmation.
- **After WFO check-in:** limited geofence monitoring supports auto-checkout only.
- **During WFH sessions:** no continuous location tracking.

Final daily attendance mode is **WFO or WFH only** (no HYBRID). Times are stored in **UTC** and displayed in the **browser local timezone** (e.g. IST for users in India).

---

## Key Features

| Area | Features |
|------|----------|
| **Attendance** | Auto WFO check-in, WFH confirmation prompt, manual check-in/out, same-day re-check-in, WFO auto-checkout monitoring |
| **Geo-fencing** | PostGIS geofence validation against assigned office; Redis-cached office lookup |
| **Summaries** | Daily `attendance_records` with office minutes and final WFO/WFH mode |
| **History** | Full `attendance_events` and `attendance_sessions` audit trail |
| **Dashboards** | Employee, Manager, Leadership role-specific views |
| **Admin** | Employee, office location, and attendance policy management |
| **Outliers** | Rule-based anomaly detection (late, absence, missing checkout, low WFO, repeated EOD close) |
| **Notifications** | In-app alerts for managers and employees |
| **Async processing** | Transactional outbox + scheduled jobs for classification, outliers, cache refresh |
| **API docs** | Swagger/OpenAPI with paginated list APIs |

---

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 3.x, Spring Security (JWT), JPA, Flyway |
| Frontend | React, Vite, React Router, Axios, Recharts, PWA-ready |
| Database | PostgreSQL 16 + PostGIS |
| Cache / locks | Redis, Redisson |
| API docs | Springdoc OpenAPI / Swagger UI |
| Deployment | Docker Compose |

---

## Architecture Summary

```
React PWA  ──HTTPS/JWT──►  Spring Boot Modular Monolith
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              PostgreSQL         Redis          Outbox + Schedulers
              + PostGIS       (cache/locks)    (async side effects)
```

| Component | Role |
|-----------|------|
| **PostgreSQL/PostGIS** | Source of truth — employees, offices, attendance, outliers |
| **Redis** | Assigned office cache, auto-tracking state, dashboard cache, distributed locks |
| **Outbox poller** | Async classification, outlier detection, notifications, cache refresh |
| **Day-close scheduler** | EOD system close for open sessions at 23:59:59 |

See [docs/architecture.md](docs/architecture.md) for the full architecture diagram and module breakdown.

---

## Diagrams

### Project scope (high level)

```mermaid
flowchart TD
    User["Employee Manager Leadership Admin"] --> Web["React PWA Frontend"]
    Web --> Auth["Authentication and RBAC"]
    Web --> Attendance["Attendance Module"]
    Web --> Dashboard["Dashboard Module"]
    Web --> Admin["Admin Configuration"]
    Attendance --> Geofence["Geofence Validation"]
    Attendance --> Events["Attendance Events"]
    Attendance --> Summary["Daily Attendance Summary"]
    Attendance --> Outlier["Outlier Detection"]
    Geofence --> PostGIS["PostgreSQL PostGIS"]
    Geofence --> Redis["Redis Office Cache"]
```

### Employee attendance flow (summary)

```mermaid
flowchart TD
    Start["Open Employee Dashboard"] --> Status{"Active session?"}
    Status -->|WFO| Monitor["Auto checkout monitoring"]
    Status -->|WFH| Manual["Manual checkout or EOD close"]
    Status -->|No| Loc["Get location once"]
    Loc --> Geo{"Inside assigned office?"}
    Geo -->|Yes| AutoWFO["Auto check in as WFO"]
    Geo -->|No| Prompt["WFH confirmation prompt"]
    AutoWFO --> Monitor
    Prompt -->|Confirm| WFH["Check in as WFH"]
    WFH --> Manual
```

Detailed flow: [docs/attendance-flow.md](docs/attendance-flow.md)

---

## Final Attendance Flow

### Before check-in

1. Employee opens the dashboard (or returns after checkout).
2. App calls `getCurrentPosition()` **once** and sends location to the backend.
3. **Inside** assigned office geofence → auto **WFO** check-in after ~15s stability (demo).
4. **Outside** assigned office geofence → **WFH confirmation prompt** (WFH is never silently marked).
5. Manual check-in remains available; backend classifies WFO/WFH from geofence.

### Active WFO session

- UI: *Checked in as WFO* + *Auto-checkout monitoring active*.
- Limited `watchPosition` runs **only** for auto-checkout.
- Auto-checkout after continuous time outside geofence (60s demo; 15–30 min production).
- Poor/stale GPS readings do not trigger checkout.
- Manual checkout always available.

### Active WFH session

- **No** continuous location monitoring.
- UI: *Checked in as WFH* + *Manual checkout required*.
- Manual checkout or **EOD system close** if checkout is missed.

### Same-day re-check-in

After checkout, check-in is enabled again and the same location evaluation flow runs.

### Daily summary

- `total_office_minutes` = sum of WFO session durations.
- Final mode: **WFO** if `total_office_minutes >= required_wfo_minutes` (default 180), else **WFH**.
- **No HYBRID** daily status in MVP.

Full rules: [docs/attendance-flow.md](docs/attendance-flow.md)

---

## Demo Users

All seeded users share the password **`password`**.

| Email | Role | Notes |
|-------|------|-------|
| employee@demo.com | EMPLOYEE | Assigned to EY Bengaluru - Ecospace; today left open for live testing |
| manager@demo.com | MANAGER | Engineering team manager dashboard |
| leader@demo.com | LEADERSHIP | Organization-wide trends |
| admin@demo.com | ADMIN | Employee, office, and policy administration |

### Demo office (employee@demo.com)

| Field | Value |
|-------|-------|
| Office | **EY Bengaluru - Ecospace** |
| Address | Campus 1C, Ecospace Business Park, Bellandur, Outer Ring Road, Bengaluru, Karnataka 560103 |
| Latitude | 12.9262 |
| Longitude | 77.6811 |
| Geofence radius | 500 meters |

Additional demo employees (~100 total) are seeded across five teams. See `backend/src/main/resources/demo-data/`.

---

## Local Setup

**Recommended:** run the full stack with Docker Compose.

```bash
docker compose up --build
```

**Reset database and re-seed:**

```bash
docker compose down -v
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

See [docs/local-setup.md](docs/local-setup.md) for DBeaver connection, geolocation testing, and troubleshooting.

---

## Swagger / API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Login via `POST /api/auth/login`, then authorize in Swagger with `Bearer <token>`.

API reference: [docs/api-design.md](docs/api-design.md)

---

## Dashboards

| Role | API | Data source |
|------|-----|-------------|
| Employee | `GET /api/employee/dashboard-summary` | Personal KPIs, assigned office, today status, 30-day trend |
| Manager | `GET /api/manager/dashboard-summary` | Team KPIs, WFO/WFH counts, outliers |
| Leadership | `GET /api/leadership/dashboard` | Organization-wide aggregates and trends |

Dashboards read from **`attendance_records`** daily summaries. Drill-down uses paginated session and event APIs.

---

## Security and Privacy Notes

- **JWT authentication** with role-based access control (EMPLOYEE, MANAGER, LEADERSHIP, ADMIN).
- Location is used **only for attendance classification** while the app is open.
- **No background location tracking** when the browser tab or PWA is closed.
- Browser location can be spoofed; backend validates geofence and audit/outlier rules flag suspicious patterns.
- Demo JWT auth — production would use enterprise SSO (OIDC).

---

## Known Limitations

| Limitation | Impact |
|------------|--------|
| PWA foreground only | Auto-checkout requires app open with location permission |
| One office per employee | Multi-office assignment is a future enhancement |
| No native mobile app | No OS-level background geofencing |
| In-app notifications only | No email/SMS/push |
| HTTP polling | No WebSocket/SSE for live updates |
| Demo auth | Not production SSO |
| Location spoofing | Mitigated by audit/outlier detection; production may add Wi-Fi/badge/MDM |

There is **no** "Enable auto attendance" toggle, and **no** simulate inside/outside office buttons in the MVP UI.

---

## Future Enhancements

- Multiple office assignment via `employee_office_assignments`
- Native mobile app with background geofencing
- Corporate Wi-Fi / badge validation
- Kafka or event streaming (replacing in-process outbox poller at scale)
- Enterprise SSO (OIDC)
- Advanced analytics and HRMS integration
- Optional HYBRID or richer daily classification labels

See [docs/tradeoffs.md](docs/tradeoffs.md) and [docs/project-scope.md](docs/project-scope.md).

---

## Project Structure

```
wfh-wfo-attendance-app/
├── backend/          # Spring Boot modular monolith
├── frontend/         # React PWA
├── docs/             # Architecture, API, attendance flow, scope, trade-offs
├── screenshots/      # UI screenshots
├── docker-compose.yml
└── README.md
```

---

## Running Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`.

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/project-scope.md](docs/project-scope.md) | In-scope, out-of-scope, future enhancements |
| [docs/architecture.md](docs/architecture.md) | Modular monolith, Redis, outbox, schedulers |
| [docs/attendance-flow.md](docs/attendance-flow.md) | Final check-in/check-out flow with diagrams |
| [docs/api-design.md](docs/api-design.md) | REST API reference and pagination |
| [docs/tradeoffs.md](docs/tradeoffs.md) | Design decisions and alternatives |
| [docs/local-setup.md](docs/local-setup.md) | Docker setup and local testing |
| [docs/assumptions.md](docs/assumptions.md) | Product and technical assumptions |
