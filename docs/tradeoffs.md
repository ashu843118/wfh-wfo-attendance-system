# Trade-offs

Design decisions and alternatives considered for the WFH/WFO Attendance MVP.

## One office per employee vs multiple offices

| Choice | Rationale |
|--------|-----------|
| **One assigned office (MVP)** | Simple mental model, single Redis cache key, straightforward geofence rules for evaluators |
| **Multiple offices (future)** | Supports roaming staff, hot-desking, and multi-campus employees via `employee_office_assignments` |

## Redis cache vs database lookup on every signal

| Choice | Rationale |
|--------|-----------|
| **Redis cache with TTL + invalidation** | Location signals arrive frequently while the app is open; caching assigned office metadata avoids repeated joins |
| **DB always** | Correct but adds load; acceptable only at very small scale |
| **Cache is not source of truth** | PostgreSQL remains authoritative; stale cache is bounded by TTL and evicted on admin updates |

## Auto WFO vs confirmed WFH

| Choice | Rationale |
|--------|-----------|
| **Auto WFO inside assigned geofence** | Physical presence at the assigned office is strong evidence of WFO work |
| **Confirmed WFH outside geofence** | Outside the fence could mean home, client site, travel, leave, or GPS error — silent WFH would be inaccurate and unfair |

## Auto checkout only for auto WFO sessions

| Choice | Rationale |
|--------|-----------|
| **Auto checkout for auto WFO only** | Employee explicitly chose manual/WFH path; predictable expectation that they also check out explicitly |
| **Auto checkout for all sessions** | Would surprise users who confirmed WFH from home and closed the laptop without checking out |

Manual/WFH sessions fall back to **manual checkout** or **EOD system close** (23:59:59) with `MISSING_CHECKOUT` handling.

## Session/event history vs daily summary table

| Choice | Rationale |
|--------|-----------|
| **Events + sessions for detail** | Supports same-day re-check-in, audit, and drill-down |
| **Daily summary (`attendance_records`) for dashboards** | Fast manager/leadership aggregations without scanning all events |
| **Derived fields on summary** | `first_check_in_time`, `final_check_out_time`, `total_office_minutes`, final `attendance_mode` computed from sessions |

## WFO/WFH only vs HYBRID daily mode

| Choice | Rationale |
|--------|-----------|
| **WFO or WFH only on dashboards (MVP)** | Simple reporting for managers and leadership |
| **HYBRID label (deferred)** | `total_office_minutes` already captures split days; a richer label can be added later without losing data |

Final mode rule: **WFO if `total_office_minutes >= required_wfo_minutes`, else WFH.**

## Modular monolith vs microservices

| Choice | Rationale |
|--------|-----------|
| **Modular monolith** | Single Docker Compose stack, ACID transactions across attendance + outbox + sessions, easy assignment evaluation |
| **Microservices (future)** | Extract attendance, notification, or analytics when team/size boundaries require independent scaling |

## Transactional outbox vs Kafka

| Outbox (MVP) | Kafka (future) |
|--------------|----------------|
| No extra infrastructure | Better for high-volume multi-service fan-out |
| Same DB transaction as attendance write | Decouples producers/consumers at scale |
| Spring `@Scheduled` poller | Replace poller with Kafka consumers |

## Async classification vs synchronous geofence on every write

| Choice | Rationale |
|--------|-----------|
| **Fast write + async classification for some paths** | Better PWA UX; daily mode and late flags can update shortly after check-in |
| **Synchronous geofence for location-signal auto flow** | Auto WFO/WFH session creation needs immediate geofence result |
| **Trade-off** | Dashboards may briefly show `CLASSIFICATION_PENDING` after manual check-in |

## Polling vs WebSocket/SSE

| Choice | Rationale |
|--------|-----------|
| **HTTP polling (MVP)** | Simple, works through standard proxies, sufficient for demo |
| **WebSocket/SSE (future)** | Lower latency for live team dashboards and notifications |

## PWA vs native mobile app

| Choice | Rationale |
|--------|-----------|
| **PWA / responsive web** | Zero app-store friction for evaluators; works on desktop and mobile browsers |
| **Native app (future)** | True background geofencing, better OS-level location APIs, offline queueing |

The MVP intentionally tracks location **only while the app is open** — a browser limitation accepted for assignment scope.

## PostGIS vs application-only Haversine

| Choice | Rationale |
|--------|-----------|
| **PostGIS primary** | Spatial indexes, consistent distance semantics, co-located with relational data |
| **Java Haversine** | Display/helper only; not the authority for fence decisions |

## Demo seed: CSV + generator vs manual SQL

| Choice | Rationale |
|--------|-----------|
| **CSV reference data + deterministic Java generator** | Reproducible demo, realistic volume (~100 employees, 30 days), idempotent skip on restart |
| **Static SQL dumps** | Harder to maintain and review |

## Removed / out-of-scope UI (MVP)

The following were **not** included in the final MVP to keep the employee experience aligned with real browser geolocation:

- No **"Enable auto attendance while app is open"** toggle (auto attendance is always on while the dashboard is open).
- No **"Simulate inside office"** or **"Simulate outside office"** demo buttons.
- No **HYBRID** daily status on dashboards.

## Evolution path

1. Multiple office assignments per employee
2. Kafka (or similar) replacing in-process outbox poller
3. Enterprise SSO (OIDC)
4. Real-time push notifications
5. Native mobile app with background geofencing
6. Optional HYBRID or richer daily classification labels
7. Module extraction to microservices behind an API gateway
