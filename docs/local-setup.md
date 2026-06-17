# Local Setup

Guide for running and evaluating the WFH/WFO Attendance Tracking App locally.

---

## Prerequisites

- **Docker Desktop** (recommended full stack)
- Optional for manual dev mode: Java 17, Node.js 20+, Maven wrapper in `backend/`

Evaluators do **not** need to install PostgreSQL, PostGIS, or Redis locally when using Docker Compose.

---

## 1. Run with Docker Compose

From the repository root:

```bash
docker compose up --build
```

Wait until all services are healthy (backend healthcheck passes).

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

---

## 2. Reset Database (Fresh Demo Seed)

Removes PostgreSQL volume, re-runs Flyway migrations, and re-seeds demo data:

```bash
docker compose down -v
docker compose up --build
```

Use this when you want a clean 30-day history or after schema migrations change.

---

## 3. Demo Login

All seeded users share the password **`password`**.

| Email | Password | Role |
|-------|----------|------|
| employee@demo.com | password | Employee |
| manager@demo.com | password | Manager |
| leader@demo.com | password | Leadership |
| admin@demo.com | password | Admin |

### Demo employee office assignment

`employee@demo.com` is mapped to **EY Bengaluru - Ecospace**:

| Field | Value |
|-------|-------|
| Address | Campus 1C, Ecospace Business Park, Bellandur, Outer Ring Road, Bengaluru, Karnataka 560103 |
| Latitude | 12.9262 |
| Longitude | 77.6811 |
| Geofence radius | 100 meters (EY Bengaluru demo office) |

---

## 4. DBeaver / SQL Client Connection

Postgres is exposed on host port **5433** (container internal port is 5432).

| Setting | Value |
|---------|-------|
| Host | localhost |
| Port | 5433 |
| Database | attendance_db |
| User | attendance_user |
| Password | attendance_pass |

### Useful tables for evaluation

| Table | Purpose |
|-------|---------|
| `attendance_records` | Daily summary (final WFO/WFH mode, office minutes) |
| `attendance_sessions` | Work sessions (WFO/WFH, open/closed, session mode) |
| `attendance_events` | Full event audit trail |
| `employees` | Includes `assigned_office_location_id` |
| `office_locations` | Geofence center + radius |
| `attendance_policies` | Includes `required_wfo_minutes` |
| `outbox_events` | Async processing queue |
| `attendance_outliers` | Detected anomalies |

---

## 5. Swagger / OpenAPI

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Use Swagger to explore attendance, dashboard, admin, and notification endpoints:

1. Login via `POST /api/auth/login`
2. Copy the JWT token from the response
3. Click **Authorize** in Swagger and enter `Bearer <token>`

---

## 6. Testing Location Behavior Locally

The Employee Dashboard **automatically requests browser location permission** on load. There is:

- **No** "Enable auto attendance" toggle
- **No** "Simulate inside office" or "Simulate outside office" buttons

### Expected behavior

| Your location vs assigned office | Result |
|----------------------------------|--------|
| Inside assigned office geofence | After ~15s stability, auto WFO check-in |
| Outside assigned office geofence | WFH confirmation prompt (WFH is not silent) |
| Permission denied | Fallback message; manual check-in/out still works |

### WFO session (after check-in)

- UI shows *Checked in as WFO* and *Auto-checkout monitoring active*
- Limited geofence watcher runs for auto-checkout only
- Auto checkout (demo) after **60 seconds** continuously outside the fence

### WFH session (after WFH check-in)

- **No** continuous location monitoring
- Manual checkout required, or EOD system close at 23:59:59

### If you are not physically near the office

If your GPS is outside the EY Bengaluru geofence, the **WFH confirmation prompt is expected behavior**.

Options for local WFO testing:

1. **Allow location** and physically move inside the geofence (**100 m** radius around 12.9262, 77.6811 for EY Bengaluru), or
2. Log in as **admin@demo.com** → edit the assigned office lat/lng/radius to match your area, or
3. Reassign the employee to a nearer demo office.

### Browser requirements

- Use **localhost** or **HTTPS** (browsers block geolocation on insecure remote origins).
- Chrome/Edge recommended for Geolocation API testing.
- Location works **only while the app tab is open** — closing the browser stops auto-checkout monitoring.

---

## 7. Manual Development Mode (Optional)

Start infrastructure only:

```bash
docker compose up -d postgres redis
```

**Backend** (profile `docker-local` connects to localhost:5433):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker-local
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=docker-local"
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

Create `frontend/.env.local`:

```
VITE_API_BASE_URL=http://localhost:8080
```

Frontend dev server: http://localhost:5173 (or port shown by Vite).

---

## 8. Running Tests

```bash
cd backend
./mvnw test

cd frontend
npm run build
```

On Windows use `.\mvnw.cmd`.

---

## 9. Spring Profiles

| Profile | Use case |
|---------|----------|
| `docker` | Full Docker Compose stack (`postgres`, `redis` hostnames) |
| `docker-local` | Backend on host, Postgres/Redis in Docker on localhost:5433 |
| `prod` | Hosted deployment via environment variables |

The base `application.yaml` has no datasource — always activate a profile when running the backend outside Docker.

Demo seed is enabled in Docker via `app.demo.seed.enabled: true` in `application-docker.yaml`.

---

## 10. Configuration Reference (Demo Values)

| Setting | Demo default | Production recommendation |
|---------|--------------|---------------------------|
| `app.attendance.auto.check-in-stable-seconds` | 15 | 120–300 (2–5 min) |
| `app.attendance.auto.checkout-grace-seconds` | 60 | 900–1800 (15–30 min) |
| `app.attendance.auto.max-accuracy-meters` | 100 | Tune per environment |
| `app.attendance.day-close.default-close-time` | 23:59:59 | Per org policy |
| `app.attendance.day-close.cron` | `0 5 0 * * *` | Adjust timezone as needed |
| `required_wfo_minutes` (policy) | 180 | Per team policy |

---

## 11. Troubleshooting

| Issue | Check |
|-------|-------|
| Backend not healthy | `docker compose logs backend`; wait for Flyway migrations |
| Location signal 400 | Request must include `timestamp` on location payload |
| Auto check-in never fires | Confirm inside assigned office for ~15s with app open; check no open session already exists |
| WFH prompt every time | Normal when outside fence; dismiss with "Not now" or confirm WFH |
| "Detecting location" after check-in | Should not persist — WFO shows stable monitoring text; refresh if stale build |
| Stale office after admin edit | Cache TTL ~15 min; restart backend or wait for invalidation |
| Empty demo data | Run `docker compose down -v` and rebuild |
| Times look wrong | Backend stores UTC; UI displays browser local timezone |

---

## Related Documentation

- [README.md](../README.md) — project overview and final attendance flow
- [attendance-flow.md](attendance-flow.md) — check-in/check-out rules
- [architecture.md](architecture.md) — Redis, outbox, schedulers
- [api-design.md](api-design.md) — REST API reference
