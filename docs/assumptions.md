# Architecture assumptions

## Employee office assignment (MVP)

Each employee has exactly one assigned office via `employees.assigned_office_location_id`.

Geofence validation for attendance (auto check-in, manual check-in, location signals) uses **only** the employee's assigned office, not the nearest active office globally.

### Future extension: multiple offices per employee

Do not implement in MVP. When needed, introduce an `employee_office_assignments` table:

| Column | Purpose |
|--------|---------|
| `employee_id` | FK to employees |
| `office_location_id` | FK to office_locations |
| `is_primary` | Primary office for default geofence |
| `active` | Soft enable/disable |

Migration path:

1. Create `employee_office_assignments` and backfill from `assigned_office_location_id`.
2. Update admin UI to manage multiple assignments with one primary.
3. Extend Redis cache key strategy (e.g. primary office cache + optional list cache).
4. Update geofence services to evaluate against primary or all active assignments per policy.

PostgreSQL/PostGIS remains the source of truth; Redis (`office:employee:{employeeId}`) remains a performance cache only.
