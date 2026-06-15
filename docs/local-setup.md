# Local Setup

Guide for running and evaluating the WFH/WFO Attendance Tracking App locally.

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
| Health | http://localhost:8080/actuator/health |

### Demo login

| Email | Password | Role |
|-------|----------|------|
| employee@demo.com | password | Employee |
| manager@demo.com | password | Manager |
| leader@demo.com | password | Leadership |
| admin@demo.com | password | Admin |

---

## 2. Reset database (fresh demo seed)

Removes PostgreSQL volume, re-runs Flyway migrations, and re-seeds demo data:

```bash
docker compose down -v
docker compose up --build
```

Use this when you want a clean 30-day history or after schema migrations change.

---

## 3. DBeaver / SQL client connection

Postgres is exposed on host port **5433** (container internal port is 5432).

| Setting | Value |
|---------|-------|
| Host | localhost |
| Port | 5433 |
| Database | attendance_db |
| User | attendance_user |
| Password | attendance_pass |

Useful tables for evaluation:

| Table | Purpose |
|-------|---------|
| `attendance_records` | Daily summary (final WFO/WFH mode, office minutes) |
| `attendance_sessions` | Work sessions (WFO/WFH, open/closed, auto-checkout flag) |
| `attendance_events` | Full event audit trail |
| `employees` | Includes `assigned_office_location_id` |
| `office_locations` | Geofence center + radius |
| `attendance_policies` | Includes `required_wfo_minutes` |

---

## 4. Swagger / OpenAPI

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Use Swagger to explore attendance, dashboard, admin, and notification endpoints with JWT auth (login first, then authorize with `Bearer <token>`).

---

## 5. Testing location behavior locally

The Employee Dashboard **automatically requests browser location permission** on login. There is no simulate-inside/outside toggle in the final MVP UI.

### Expected behavior

| Your location vs assigned office | Result |
|----------------------------------|--------|
| Inside assigned office geofence | After ~15s stability, auto WFO check-in |
| Outside assigned office geofence | WFH confirmation prompt (WFH is not silent) |
| Permission denied | Fallback message; manual check-in/out still works |

### Seeded office coordinates

Demo offices (from `backend/src/main/resources/demo-data/office_locations.csv`):

| Office | Latitude | Longitude | Radius (m) |
|--------|----------|-----------|------------|
| Pune Tech Park | 18.5912 | 73.7389 | 800 |
| Mumbai BKC | 19.0596 | 72.8656 | 750 |
| Bangalore EC | 12.8458 | 77.6658 | 700 |
| Hyderabad HITEC | 17.4485 | 78.3908 | 750 |
| Delhi Cyber Hub | 28.4945 | 77.0895 | 700 |

`employee@demo.com` is assigned to **Pune Tech Park** (Engineering team).

### Tips for local testing

1. **Allow location** when the browser prompts on the Employee Dashboard.
2. **Auto WFO only works** if your current GPS position is inside your employee's assigned office radius.
3. If you are physically far from seeded offices:
   - Log in as **admin@demo.com** → **Office Locations** → edit the assigned office lat/lng/radius to match your current area, **or**
   - Update the employee's assigned office to one near you.
4. **Auto checkout (demo)** triggers after **60 seconds** continuously outside the fence (configurable in `application.yaml`).
5. **Manual check-in/out** works regardless of auto flow and uses the same geofence classification rules.
6. Location is used **only while the app tab is open** — closing the browser stops location signals.

### Browser requirements

- Use **localhost** or **HTTPS** (browsers block geolocation on insecure remote origins).
- Chrome/Edge recommended for Geolocation API testing.

---

## 6. Manual development mode (optional)

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

Create `frontend/.env.local` with:

```
VITE_API_BASE_URL=http://localhost:8080
```

Frontend dev server: http://localhost:5173 (or port shown by Vite).

---

## 7. Running tests

```bash
cd backend
./mvnw test

cd frontend
npm run build
```

---

## 8. Spring profiles

| Profile | Use case |
|---------|----------|
| `docker` | Full Docker Compose stack (`postgres`, `redis` hostnames) |
| `docker-local` | Backend on host, Postgres/Redis in Docker on localhost:5433 |
| `prod` | Hosted deployment via environment variables |

The base `application.yaml` has no datasource — always activate a profile when running the backend outside Docker.

---

## 9. Troubleshooting

| Issue | Check |
|-------|-------|
| Backend not healthy | `docker compose logs backend`; wait for Flyway migrations |
| Location signal 400 | Request must include `timestamp` on location payload |
| Auto check-in never fires | Confirm inside assigned office for ~15s with app open; check open session already exists |
| WFH prompt every time | Normal when outside fence; dismiss with "Not now" or confirm WFH |
| Stale office after admin edit | Cache TTL is ~15 min; restart backend or wait for invalidation |
| Empty demo data | Run `docker compose down -v` and rebuild |

See [README.md](../README.md) and [architecture.md](architecture.md) for product behavior details.
