# Trade-offs

Design decisions and alternatives considered for the WFH/WFO Attendance MVP. This document reflects the **final implemented** behavior.

---

## 1. PWA vs Native App

| Choice | Rationale |
|--------|-----------|
| **PWA / responsive web (MVP)** | Zero app-store friction for evaluators; works on desktop and mobile browsers; assignment-friendly |
| **Native app (future)** | True background geofencing, better OS-level location APIs, offline queueing, device integrity checks |

**Accepted limitation:** Location and auto-checkout work **only while the app/PWA is open**. Closing the browser stops geofence monitoring. EOD system close handles missed checkouts.

There is no simulate inside/outside office button — real browser geolocation is required.

---

## 2. Auto WFO vs Confirmed WFH

| Choice | Rationale |
|--------|-----------|
| **Auto WFO inside assigned geofence** | Physical presence at the assigned office is strong evidence of WFO work |
| **Confirmed WFH outside geofence** | Outside the fence could mean home, client site, travel, leave, or GPS error — silent WFH would be inaccurate and unfair |

WFH is **never silently marked**. The user must confirm via the WFH prompt or manual check-in classification.

---

## 3. One Assigned Office vs Multiple Offices

| Choice | Rationale |
|--------|-----------|
| **One office per employee (MVP)** | Simple mental model, single Redis cache key (`office:employee:employeeId`), straightforward geofence rules |
| **Multiple offices (future)** | Supports roaming staff, hot-desking, multi-campus via `employee_office_assignments` |

---

## 4. Redis Cache vs DB Lookup

| Choice | Rationale |
|--------|-----------|
| **Redis cache with TTL + invalidation** | Location signals during WFO monitoring and check-in evaluation benefit from fast office metadata lookup |
| **Optional today attendance cache** | Short TTL cache (`attendance:today:employeeId:date`) speeds dashboard status refresh |
| **DB always** | Correct but adds load on every signal; acceptable only at very small scale |
| **Cache is not source of truth** | PostgreSQL/PostGIS remains authoritative for active sessions and daily summaries; stale cache bounded by TTL and evicted on writes |

Redis also stores ephemeral auto-tracking state and dashboard cache entries — not durable attendance data.

Two primary Redis usages:

1. **Assigned office cache** — key `office:employee:employeeId` for geofence validation.
2. **Today attendance status cache** — key `attendance:today:employeeId:date` for optional fast dashboard lookup; evicted on check-in, checkout, auto-checkout, WFH confirmed check-in, and EOD close.

---

## 5. Session/Event History vs Daily Summary

| Choice | Rationale |
|--------|-----------|
| **Events + sessions for detail** | Supports same-day re-check-in, full audit trail, geofence enter/exit history, **per-session WFO/WFH mode** |
| **Daily summary (`attendance_records`) for dashboards** | Fast manager/leadership aggregations using **final daily mode** without scanning all events |
| **Derived fields on summary** | `first_check_in_time`, `final_check_out_time`, `total_office_minutes` (WFO sessions only), final `attendance_mode` computed from sessions |
| **DB source of truth for active session** | Active session state read from PostgreSQL; Redis today cache is optional |

---

## 5a. Attendance Module vs Outbox

| Choice | Rationale |
|--------|-----------|
| **Attendance Module saves core data directly** | `attendance_events`, `attendance_sessions`, and `attendance_records` written in the same transaction as the API response |
| **Outbox for async side effects only** | Notifications, dashboard cache refresh, outlier detection, manager alerts — not the main attendance record |
| **Transactional outbox** | Reliable async processing without losing attendance writes |

---

## 6. WFO/WFH Only vs HYBRID Daily Mode

| Choice | Rationale |
|--------|-----------|
| **WFO or WFH only on dashboards (MVP)** | Simple reporting for managers and leadership |
| **Session mode per check-in** | Multiple sessions per day may differ (e.g. WFO morning, WFH afternoon) |
| **HYBRID label (deferred)** | `total_office_minutes` already captures split days; richer labels can be added later |

Final **daily** mode rule: **WFO if `total_office_minutes >= required_wfo_minutes`, else WFH.** Session mode is stored separately on each check-in.

No HYBRID daily status appears on any dashboard in MVP.

---

## 7. Auto-Checkout Scope

| Choice | Rationale |
|--------|-----------|
| **Auto-checkout for WFO sessions only** | Employee at the office may leave without manually checking out; geofence exit is a reasonable signal |
| **No auto-checkout for WFH** | User confirmed WFH from outside the office; continuous location tracking would be invasive and unnecessary |

Both **auto** and **manual** WFO check-ins start auto-checkout monitoring. WFH sessions require manual checkout or EOD system close.

There is no "Enable auto attendance" toggle — WFO monitoring starts automatically when a WFO session is open.

---

## 8. Location Spoofing

| Mitigation (MVP) | Limitation |
|------------------|------------|
| Backend geofence validation against assigned office | Browser GPS can be spoofed in dev tools |
| Full event audit trail (`attendance_events`) | No device integrity attestation |
| Outlier rules flag suspicious patterns | No Wi-Fi/badge cross-check |

**Production extensions:** corporate Wi-Fi fingerprinting, badge readers, MDM device attestation, native app with OS-level location APIs, velocity/anomaly ML.

---

## 9. Outbox/Scheduler vs Kafka

| Outbox + `@Scheduled` (MVP) | Kafka (future) |
|-----------------------------|----------------|
| No extra infrastructure | Better for high-volume multi-service fan-out |
| Same DB transaction as attendance write | Decouples producers/consumers at scale |
| Spring poller every ~5s | Replace poller with Kafka consumers |
| Sufficient for demo and moderate load | Needed when attendance writes exceed single-node throughput |

Processors today: classification, outlier detection, notifications, dashboard cache refresh, audit.

---

## 10. Purpose-Driven Location vs Continuous Tracking

| Choice | Rationale |
|--------|-----------|
| **One location read before check-in** | Minimal privacy footprint; sufficient for WFO/WFH decision |
| **Limited watcher only during WFO session** | Supports auto-checkout without tracking WFH employees at home |
| **No tracking when app closed** | Respects browser/PWA platform limits and user privacy expectations |

Previously considered continuous tracking for all open sessions — rejected in favor of purpose-driven use.

---

## 11. Modular Monolith vs Microservices

| Choice | Rationale |
|--------|-----------|
| **Modular monolith** | Single Docker Compose stack, ACID transactions, easy assignment evaluation |
| **Microservices (future)** | Extract attendance, notification, or analytics when scaling requires independent deployment |

---

## 12. Async Classification vs Synchronous Writes

| Choice | Rationale |
|--------|-----------|
| **Fast write + async classification** | Better PWA UX; daily mode and late flags update shortly after check-in |
| **Synchronous geofence for location-signal flow** | Auto WFO/WFH session creation needs immediate geofence result |
| **Trade-off** | Dashboards may briefly show `CLASSIFICATION_PENDING` after manual check-in |

---

## 13. Polling vs WebSocket/SSE

| Choice | Rationale |
|--------|-----------|
| **HTTP polling (MVP)** | Simple, works through standard proxies, sufficient for demo |
| **WebSocket/SSE (future)** | Lower latency for live team dashboards and notifications |

---

## 14. PostGIS vs Application-Only Haversine

| Choice | Rationale |
|--------|-----------|
| **PostGIS primary** | Spatial indexes, consistent distance semantics, co-located with relational data |
| **Java Haversine** | Display/helper only; not authority for fence decisions |

---

## 15. Office Geofence Radius (50–300 m)

| Choice | Rationale |
|--------|-----------|
| **100 m default (MVP)** | Narrow enough for office-level validation; wide enough for typical browser GPS inaccuracy inside buildings |
| **50 m minimum** | Prevents unrealistically tight fences that would false-negative most mobile GPS readings |
| **300 m maximum** | Prevents overly broad fences that include nearby roads, cafés, or parking areas |
| **Admin-configurable** | Different campuses need different radii; production can tune per office |

The **EY Bengaluru - Ecospace** demo office uses **100 meters**. A 500 m default was too broad for meaningful office attendance validation.

---

## 16. UTC Storage vs Local Display

| Choice | Rationale |
|--------|-----------|
| **Store UTC in backend** | Consistent server-side timestamps across timezones |
| **Display in browser local timezone** | Users see check-in/out in their local time (e.g. IST) without server-side timezone configuration per user |

---

## Removed / Out-of-Scope UI (MVP)

The following were **not** included in the final MVP:

- No **"Enable auto attendance while app is open"** toggle
- No **"Simulate inside office"** or **"Simulate outside office"** demo buttons
- No **HYBRID** daily status on dashboards
- No claim of **background tracking** when browser/app is closed

---

## Evolution Path

1. Multiple office assignments per employee
2. Native mobile app with background geofencing
3. Kafka (or similar) replacing in-process outbox poller
4. Enterprise SSO (OIDC)
5. Real-time push notifications (email/SMS/push)
6. Corporate Wi-Fi / badge validation
7. Optional HYBRID or richer daily classification labels
8. Module extraction to microservices behind an API gateway

---

## Related Documentation

- [project-scope.md](project-scope.md) — in-scope and future enhancements
- [attendance-flow.md](attendance-flow.md) — implemented check-in/check-out rules
- [architecture.md](architecture.md) — Redis, outbox, schedulers
