# Architecture

## Overview

The WFH/WFO Attendance Tracking application is a **modular monolith**: one deployable Spring Boot application with clear domain package boundaries and a React PWA frontend.

```
┌─────────────┐     HTTPS/JWT      ┌──────────────────────────────────────────────┐
│  React PWA  │ ◄────────────────► │        Spring Boot Modular Monolith           │
│   (Vite)    │   location signal  │ auth │ attendance │ geofence │ dashboard │ … │
└─────────────┘                    └──────────────┬───────────────┬────────────────┘
                                                  │               │
                                       ┌──────────▼──┐     ┌──────▼─────┐
                                       │ PostgreSQL  │     │   Redis    │
                                       │  + PostGIS  │     │ + Redisson │
                                       └─────────────┘     └────────────┘
```

## Modular Monolith Packages

| Module | Responsibility |
|--------|----------------|
| `auth` | JWT authentication, login rate limiting |
| `employee` / `team` | Employee and team domain, admin CRUD |
| `attendance` | Check-in/out, location signals, sessions, daily summaries |
| `geofence` | Assigned-office geofence evaluation (PostGIS) |
| `office` | Office locations, employee office assignment, Redis cache |
| `policy` | Team attendance policies (`required_wfo_minutes`, check-in times) |
| `dashboard` | Employee, Manager, Leadership, Admin aggregated APIs |
| `outlier` | Rule-based anomaly detection (Strategy pattern) |
| `notification` | In-app notifications |
| `outbox` | Transactional outbox + async processors |
| `audit` | Audit trail for admin actions |
| `demo` | CSV + deterministic demo seed |
| `common` / `config` | Shared DTOs, adapters, security, Redis/Redisson config |

## Data Ownership

| Store | Role |
|-------|------|
| **PostgreSQL / PostGIS** | Source of truth for employees, offices, policies, attendance records, sessions, events, outliers, notifications, outbox |
| **Redis** | Performance cache and ephemeral auto-tracking state only |
| **`attendance_events`** | Immutable audit of all attendance actions (check-in, checkout, geofence enter/exit, system day close) |
| **`attendance_sessions`** | Logical work sessions (WFO/WFH, open/closed, auto-checkout eligibility) |
| **`attendance_records`** | One daily summary row per employee per date for fast dashboard queries |

Dashboards read **`attendance_records`** for KPIs and trends. Drill-down uses **`attendance_sessions`** and **`attendance_events`**.

## Office Assignment Model

### MVP (implemented)

- Each employee has **exactly one assigned office**: `employees.assigned_office_location_id` → `office_locations.id`.
- All geofence validation (auto attendance, manual check-in, location signals) uses **only** the assigned office.
- Employees and managers must have an assigned office; other roles may omit it.

### Future extension

Multiple offices per employee via an `employee_office_assignments` table (`employee_id`, `office_location_id`, `is_primary`, `active`). See [assumptions.md](assumptions.md).

## Redis Office Cache

| Item | Value |
|------|-------|
| Key pattern | `office:employee:{employeeId}` |
| Cached fields | `officeLocationId`, `officeName`, `address`, `latitude`, `longitude`, `radiusMeters`, `active` |
| TTL | **900 seconds (15 minutes)** default (`app.cache.employee-office-ttl-seconds`) |
| Flow | 1) Try Redis → 2) On miss, load from PostgreSQL → 3) Store with TTL → 4) Use for geofence validation |
| Invalidation | On employee assigned-office update; on office location create/update/delete (evicts all employees mapped to that office) |

Additional Redis keys:

- `attendance:auto:session:{employeeId}:{date}` — ephemeral auto-tracking state (inside/outside since, WFH prompt dismissed)
- Dashboard summary caches (short TTL)
- Redisson distributed locks for concurrent check-in/out per employee/date

PostgreSQL/PostGIS remains authoritative if cache is stale or evicted.

## Attendance Flow

```
Employee opens dashboard
        │
        ▼
Browser location permission requested automatically
        │
        ├─ Permission denied ──► Manual check-in/out only + fallback message
        │
        ▼
Periodic location signals while app is open
        │
        ├─ Inside assigned office, no open session
        │       └─► Stable ~15s inside ──► AUTO WFO check-in (session, auto-checkout eligible)
        │
        ├─ Outside assigned office, no open session
        │       └─► WFH confirmation prompt (not silent WFH)
        │               ├─ Confirm ──► WFH session (manual checkout or EOD close)
        │               └─ Not now ──► No attendance; manual check-in still available
        │
        ├─ Manual check-in
        │       └─► Backend classifies WFO (inside fence) or WFH (outside fence)
        │
        ├─ Auto checkout (auto WFO sessions only)
        │       └─► Outside fence for grace period (~60s demo) ──► AUTO checkout
        │
        ├─ Manual checkout
        │       └─► Closes any active session
        │
        └─ Same-day re-check-in after checkout ──► Allowed (new session)
```

### Session rules

| Session created by | Auto checkout | Checkout options |
|--------------------|---------------|------------------|
| Auto WFO check-in | Yes (after grace outside fence) | Auto or manual |
| Manual check-in | No | Manual or EOD system close |
| WFH confirmed check-in | No | Manual or EOD system close |

### End-of-day close

- Scheduled job runs shortly after midnight (cron: `0 5 0 * * *`).
- Any still-open session is closed at **23:59:59** of the attendance date (default when no policy override).
- Creates `SYSTEM_DAY_CLOSE` event; daily record status → `MISSING_CHECKOUT`; outlier rule may fire.

## Daily Summary Logic

Computed in `DailySummaryService` and persisted on `attendance_records`:

| Field | Rule |
|-------|------|
| `first_check_in_time` | Earliest valid check-in event of the day |
| `final_check_out_time` | Latest valid checkout before day-end |
| `total_office_minutes` | Sum of durations of all **WFO** sessions (open WFO sessions at EOD count up to close time) |
| `current_session_status` | `NONE` / `OPEN` / `CLOSED` from session state |
| `attendance_mode` (final) | **WFO** if `total_office_minutes >= required_wfo_minutes`, else **WFH** |
| `status` | e.g. `CHECKED_IN`, `CHECKED_OUT`, `MISSING_CHECKOUT` |

**No HYBRID daily mode in MVP.** Dashboards expose only WFO or WFH as the final daily mode.

`required_wfo_minutes` is configurable per team in `attendance_policies` (default **180**).

## Async Outbox Processing

Write path returns quickly; some classification and side effects run asynchronously:

1. Check-in/out saved in same transaction as outbox event.
2. `@Scheduled` poller claims events (`FOR UPDATE SKIP LOCKED`).
3. Processors:
   - **AttendanceClassificationProcessor** — final mode, late flag, office distance
   - **OutlierDetectionProcessor** — frequent late, absence, missing checkout, low WFO, repeated system day close
   - **DashboardCacheRefreshProcessor** — evicts Redis dashboard keys
   - **NotificationProcessor** — manager/employee alerts
   - **AuditProcessor** — audit log entries

## Geofencing (PostGIS)

- GIST indexes on office and attendance geo points.
- Primary evaluation via PostGIS `ST_DWithin` / distance against assigned office coordinates and radius.
- Java distance helpers used for display only; DB is authoritative.

## Role-Based Dashboards

| Role | Data source | Highlights |
|------|-------------|------------|
| Employee | `attendance_records` + today session status | Today's mode, office minutes, check-in/out history, 30-day trend |
| Manager | Team members' `attendance_records` | WFO/WFH counts from final daily mode, team table, outliers |
| Leadership | Aggregated `attendance_records` | Company/team trends, patterns |
| Admin | CRUD APIs | Employees (with assigned office), offices, policies |

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

See [api-design.md](api-design.md) for endpoint reference.
