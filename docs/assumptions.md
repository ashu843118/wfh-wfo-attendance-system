# Assumptions

Documented assumptions behind the MVP WFH/WFO attendance implementation. If an assumption changes, update product behavior and this file together.

## Office and geofence

1. **One assigned office per employee in MVP.** `employees.assigned_office_location_id` references `office_locations.id`.
2. **Geofence validation uses only the assigned office.** The system does not pick the nearest global office.
3. **Office coordinates and radius are administratively correct.** GPS accuracy and spoofing are out of scope for MVP.
4. **PostgreSQL/PostGIS is the source of truth** for office locations and spatial queries.
5. **Redis caches assigned office details** (`office:employee:{employeeId}`) for performance only, with TTL (~15 minutes) and explicit invalidation on updates.

## Location and client behavior

6. **Browser location permission is required for automatic attendance.** Without it, employees use manual check-in/check-out.
7. **Browser/PWA cannot track location when closed.** Auto attendance runs only while the Employee Dashboard is open; there is no background geofencing in the web MVP.
8. **HTTPS or localhost** is available so the browser Geolocation API works.
9. **Client sends latitude, longitude, accuracy, and timestamp** on check-in/out and location signals.

## WFO / WFH product rules

10. **WFO can be auto-recorded** when the employee is inside the assigned office geofence (after a short inside stability period).
11. **WFH is never marked silently.** Outside the fence, the employee must confirm WFH or use manual check-in.
12. **Manual check-in classifies WFO vs WFH** from current location vs assigned office geofence at check-in time.
13. **Auto checkout applies to all active WFO sessions** (auto or manual check-in). A limited geofence watcher runs only while the app is open.
14. **WFH sessions do not use continuous location monitoring** and require manual checkout or EOD system close.
15. **Same-day multiple sessions are allowed.** Check-in is blocked only while a session is open.
16. **Final daily attendance mode is WFO or WFH only** — no HYBRID status on dashboards in MVP.
17. **A WFO day is determined by office duration:** `total_office_minutes >= required_wfo_minutes` (default **180** minutes, configurable in `attendance_policies`).
18. **Later WFH sessions do not downgrade** a day that already accumulated enough WFO minutes.

## Time and scheduling

19. **Default EOD close time is 23:59:59** on the attendance date when no explicit policy end time applies.
20. **EOD close creates a `SYSTEM_DAY_CLOSE` event** and may raise a `MISSING_CHECKOUT` outlier when the employee did not manually check out.
21. **Demo auto-checkout grace period is 60 seconds** outside the fence; production deployments should use 15–30 minutes.
22. **Inside stability before auto check-in is ~15 seconds** in the default configuration.

## Data model

23. **`attendance_events` stores the full event audit trail** (manual, auto, geofence, system).
24. **`attendance_sessions` stores logical work sessions** with mode, checkout eligibility, and open/closed state.
25. **`attendance_records` stores one daily summary per employee per date** for dashboard performance.
26. **Working days for demo seed exclude weekends.** Production holiday calendars are a future enhancement.

## Security and operations

27. **Demo JWT authentication** is sufficient for assignment evaluation; enterprise SSO is a future replacement.
28. **All employees in demo seed share password `password`.** Not acceptable for production.
29. **In-app notifications only** — no email, SMS, or push in MVP.
30. **Transactional outbox with in-process poller** is sufficient for MVP volume; Kafka is a future option.

## Future extension: multiple offices per employee

Not implemented in MVP. Planned model:

| Column | Purpose |
|--------|---------|
| `employee_id` | FK to employees |
| `office_location_id` | FK to office_locations |
| `is_primary` | Default geofence office |
| `active` | Enable/disable assignment |

Migration path:

1. Create `employee_office_assignments` and backfill from `assigned_office_location_id`.
2. Extend admin UI for multiple assignments with one primary.
3. Extend Redis cache strategy (primary office key + optional assignment list).
4. Update geofence services to evaluate primary or all active assignments per policy.
