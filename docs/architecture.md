# Architecture

## Overview

The WFH/WFO Attendance Tracking application is implemented as a **modular monolith**: a single deployable Spring Boot application with clear package boundaries that mirror domain capabilities.

```
┌─────────────┐     HTTPS/JWT      ┌──────────────────────────────────────┐
│  React PWA  │ ◄────────────────► │     Spring Boot Modular Monolith      │
│   (Vite)    │                    │  auth │ attendance │ dashboard │ ... │
└─────────────┘                    └───────────┬──────────────┬─────────────┘
                                               │              │
                                    ┌──────────▼──┐    ┌──────▼─────┐
                                    │ PostgreSQL  │    │   Redis    │
                                    │  + PostGIS  │    │ + Redisson │
                                    └─────────────┘    └────────────┘
```

## Modular Monolith Decision

All backend capabilities live in one Spring Boot process under `com.wfhwfo.attendance`:

| Module | Responsibility |
|--------|----------------|
| `auth` | JWT demo authentication |
| `employee` / `team` | User and team domain |
| `attendance` | Check-in/out, history |
| `geofence` | PostGIS WFO/WFH classification |
| `outbox` | Transactional outbox + async processors |
| `outlier` | Rule-based anomaly detection (Strategy pattern) |
| `dashboard` | Role-specific aggregated APIs |
| `notification` | In-app notifications |
| `office` / `policy` | Admin configuration |
| `audit` | Audit trail |
| `common` / `config` | Shared infrastructure |

**Why monolith for MVP:** Single Docker Compose stack, simpler local evaluation, ACID transactions across attendance + outbox, lower operational overhead.

**Future extraction path:** Each package can become a microservice behind an API gateway. The transactional outbox pattern maps naturally to Kafka/event bus in production (mentioned as future enhancement only).

## Async Outbox Flow

1. Employee checks in → attendance record saved with `CLASSIFICATION_PENDING`
2. Outbox event `ATTENDANCE_CHECKED_IN` saved in the **same DB transaction**
3. Fast HTTP response returned to client
4. `@Scheduled` poller claims pending events atomically (`FOR UPDATE SKIP LOCKED`)
5. Processors run on thread pool:
   - **AttendanceClassificationProcessor** – PostGIS `ST_DWithin` / `ST_Distance`
   - **OutlierDetectionProcessor** – strategy-based rules
   - **DashboardCacheRefreshProcessor** – evicts Redis dashboard keys
   - **NotificationProcessor** – manager/employee alerts
   - **AuditProcessor** – audit log entries

## PostgreSQL + PostGIS

- Source of truth for relational and geospatial data
- GIST indexes on `office_locations.geo_point` and attendance geo points
- Native SQL for geofence queries (not Java Haversine as primary)

## Redis Usage

- **Cache:** active office locations, manager/leadership dashboard summaries (short TTL)
- **Locks:** Redisson `RLock` for concurrent check-in/check-out per employee/date
- Redis is **not** source of truth; DB constraints guarantee correctness if locks expire

## Redisson Locking

Lock keys: `attendance:checkin:{employeeId}:{date}`, `attendance:checkout:{employeeId}:{date}`

Combined with DB unique constraint on `(employee_id, attendance_date)` and atomic outbox claiming.

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
