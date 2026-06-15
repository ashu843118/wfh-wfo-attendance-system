# Trade-offs

## Modular Monolith vs Microservices

| Choice | Rationale |
|--------|-----------|
| **Monolith (MVP)** | Easier local run, single transaction boundary, faster assignment evaluation |
| **Microservices (future)** | Extract `attendance`, `notification`, `analytics` services when scale/team boundaries require it |

## PostgreSQL/PostGIS vs NoSQL / Java-only Geofencing

| Choice | Rationale |
|--------|-----------|
| **PostGIS** | Production-grade spatial indexes, `ST_DWithin`/`ST_Distance`, ACID with attendance records |
| **NoSQL rejected** | Domain is relational; joins and aggregations dominate dashboard queries |
| **Java Haversine** | Kept as helper only; DB is primary for geofence correctness |

## Redis Caching

- Short TTL caches for office locations and dashboard summaries reduce DB load.
- Not used as source of truth; stale cache acceptable for dashboards (refreshed on classification events).

## Redisson Locks

- Prevents duplicate concurrent check-in/out across instances.
- **Still combined** with DB unique constraints and atomic outbox updates—locks can expire; DB must remain correct.

## Transactional Outbox vs Kafka (MVP)

| Outbox (chosen) | Kafka (future) |
|-----------------|----------------|
| No extra infrastructure | Better for high-volume multi-service events |
| Same DB transaction as attendance | Decouples producers/consumers at scale |
| Spring `@Scheduled` poller | Replace poller with Kafka consumers later |

## Async Classification

- **Pro:** Fast write path, better UX on mobile/PWA check-in
- **Con:** Dashboard/mode not instant; polling required
- **Note:** Physical access control would require synchronous geofence validation

## Polling vs WebSocket/SSE

- Polling chosen for MVP simplicity (notifications 30s, dashboards 60s).
- WebSocket/SSE/push can replace polling in production.

## PWA vs Native App

- PWA reaches evaluators quickly without app store deployment.
- Native apps enable richer background geofencing later.

## Evolution Path

1. Replace outbox poller with Kafka consumers
2. Split modules into services behind API gateway
3. Add enterprise SSO (OIDC)
4. Add real-time push notifications
5. Optional synchronous geofence mode for access-control use cases
