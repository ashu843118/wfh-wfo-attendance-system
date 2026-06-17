# API Design

Base URL: `http://localhost:8080`

Interactive documentation: **http://localhost:8080/swagger-ui/index.html**

All authenticated endpoints require:

```
Authorization: Bearer <JWT>
```

---

## Standard Response Wrappers

### `ApiResponse<T>`

```json
{
  "success": true,
  "message": "Operation completed",
  "data": { },
  "timestamp": "2026-06-15T10:05:01"
}
```

Errors include `success: false`, a `message`, optional `errorCode`, and optional field-level validation details in `data`.

Login failures return **401** with message `"Invalid email or password."` (does not reveal whether the email exists).

Rate limiting: max **5 failed attempts per email or client IP within 5 minutes** → **429** with `"Too many login attempts. Please try again later."`

Credentials must be sent in the **POST request body only**; query-string credentials are rejected with **400**.

### `PagedResponse<T>`

Used inside `ApiResponse.data` for list endpoints:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

### Pagination conventions

| Parameter | Default | Max | Notes |
|-----------|---------|-----|-------|
| `page` | 0 | — | Zero-based page index |
| `size` | 20 | 100 | Configured in `PaginationConfig` |

Sort order varies by endpoint (documented per section below).

---

## Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Demo login; returns JWT |
| GET | `/api/auth/me` | JWT | Current user profile |

**Login request:**

```json
{
  "email": "employee@demo.com",
  "password": "password"
}
```

**Login response `data`:**

```json
{
  "token": "eyJ...",
  "employeeId": 8,
  "email": "employee@demo.com",
  "name": "Ashutosh Kumar",
  "role": "EMPLOYEE",
  "teamId": 1,
  "managerId": 1
}
```

---

## Attendance

Core employee attendance APIs. Location payloads require `latitude`, `longitude`, `accuracy`, and `timestamp`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/attendance/me/today` | Today's daily summary + session status |
| GET | `/api/attendance/me?from=&to=&page=&size=` | Paginated daily attendance history (date **DESC**) |
| GET | `/api/attendance/history?from=&to=&page=&size=` | Alias for `/me` history |
| POST | `/api/attendance/location-signal` | Process location signal (auto WFO, WFH prompt, WFO auto-checkout) |
| POST | `/api/attendance/check-in` | Manual check-in; backend classifies WFO/WFH from geofence |
| POST | `/api/attendance/wfh-check-in` | Confirm WFH when outside assigned office |
| POST | `/api/attendance/dismiss-wfh-prompt` | Dismiss WFH confirmation prompt for today |
| POST | `/api/attendance/check-out` | Manual check-out; closes active session |
| GET | `/api/attendance/me/events?from=&to=&page=&size=` | Paginated event audit history (event time **DESC**) |
| GET | `/api/attendance/me/events/{date}` | Paginated events for a specific date (event time **ASC**) |
| GET | `/api/attendance/me/sessions?date=&page=&size=` | Paginated sessions for a date |
| POST | `/api/attendance/events/auto` | Low-level geofence enter/exit event (internal to location flow) |

> **Note:** There is no `/api/attendance/today` alias. Use **`GET /api/attendance/me/today`**.

### Location signal

**Request body (`LocationPayload`):**

```json
{
  "latitude": 12.9262,
  "longitude": 77.6811,
  "accuracy": 10,
  "timestamp": "2026-06-15T09:30:00"
}
```

**Response `data` highlights:**

```json
{
  "trackingState": "WFH_CONFIRMATION_REQUIRED",
  "insideOffice": false,
  "locationReliable": true,
  "assignedOfficeName": "EY Bengaluru - Ecospace",
  "distanceFromOfficeMeters": 14703.05,
  "requiresWfhConfirmation": true,
  "checkInStableSecondsRemaining": null,
  "graceSecondsRemaining": null,
  "userMessage": null,
  "todaySummary": { },
  "actionTaken": { }
}
```

**`trackingState` values:**

`WAITING_FOR_PERMISSION`, `LOCATION_PERMISSION_DENIED`, `DETECTING_LOCATION`, `AUTO_CHECKIN_PENDING`, `INSIDE_OFFICE`, `OUTSIDE_OFFICE`, `CHECKED_IN_WFO`, `CHECKED_IN_WFH`, `AUTO_CHECKOUT_MONITORING_ACTIVE`, `WFH_CONFIRMATION_REQUIRED`, `NOT_CHECKED_IN`, `AUTO_CHECKOUT_PENDING`, `AUTO_CHECKED_OUT`, `CHECKED_OUT`, `MISSING_CHECKOUT`, `SYSTEM_CLOSED`

### Manual check-in / check-out

**Check-in request:**

```json
{
  "location": {
    "latitude": 12.9262,
    "longitude": 77.6811,
    "accuracy": 10,
    "timestamp": "2026-06-15T09:30:00"
  },
  "source": "PWA"
}
```

**Check-out request:** same `location` wrapper shape.

**Action response `data`:**

```json
{
  "attendanceId": 1812,
  "eventId": 3546,
  "status": "CHECKED_OUT",
  "processingStatus": "COMPLETED",
  "recordedAt": "2026-06-15T06:08:10"
}
```

### Today's attendance record (`AttendanceRecordResponse`)

| Field | Meaning |
|-------|---------|
| `attendanceMode` | Final daily mode: **WFO** or **WFH** (no HYBRID) |
| `currentSessionMode` | **WFO** or **WFH** for the open session (null if no open session) |
| `currentSessionStatus` | `NONE`, `OPEN`, or `CLOSED` |
| `totalOfficeMinutes` | Sum of WFO session durations for the day |
| `checkInTime` | Earliest check-in (`first_check_in_time`, UTC) |
| `checkOutTime` | Latest checkout (`final_check_out_time`, UTC) |
| `status` | e.g. `CHECKED_IN`, `CHECKED_OUT`, `MISSING_CHECKOUT` |
| `processingStatus` | `CLASSIFICATION_PENDING` or `COMPLETED` |

Times are stored in UTC; the frontend displays them in browser local timezone.

---

## Employee Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/employee/dashboard-summary` | Personal KPIs, assigned office, recent daily records, 30-day WFO/WFH trend |

Uses **`attendance_records`** daily summaries.

> Conceptual alias: this is the **employee dashboard** API (`/api/dashboard/employee` in product terms).

---

## Manager Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/manager/dashboard-summary` | Team KPIs, charts, recent attendance |
| GET | `/api/manager/team-attendance?page=&size=&date=` | Paginated team daily records |
| GET | `/api/manager/outliers?page=&size=` | Team outlier alerts |
| GET | `/api/manager/employees/{employeeId}/attendance?from=&to=&page=&size=` | Individual employee history |

WFO/WFH counts use **final daily `attendance_mode`** from `attendance_records`.

> Conceptual alias: `/api/dashboard/manager` maps to `/api/manager/dashboard-summary`.

---

## Leadership Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/leadership/dashboard?date=` | Organization-level aggregated trends and KPIs |
| GET | `/api/leadership/team-summary?date=&page=&size=` | Paginated team performance summary |

Uses aggregated **`attendance_records`** final daily modes.

> Conceptual alias: `/api/dashboard/leadership` maps to `/api/leadership/dashboard`.

---

## Admin

Base path: `/api/admin`

### Employees

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/employees?page=&size=&search=&role=&teamId=&active=` | Paginated employee list |
| POST | `/api/admin/employees` | Create employee (includes `assignedOfficeLocationId`) |
| PUT | `/api/admin/employees/{id}` | Update employee (including assigned office) |
| PATCH | `/api/admin/employees/{id}/status` | Activate/deactivate |
| GET | `/api/admin/managers` | Active managers for dropdown |
| GET | `/api/admin/teams` | Teams for dropdown |

**Create/update employee** must include **`assignedOfficeLocationId`** for EMPLOYEE and MANAGER roles.

### Office locations

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/offices` | List offices (alias) |
| GET | `/api/admin/office-locations` | List offices |
| GET | `/api/admin/office-locations/active` | Active offices only |
| GET | `/api/admin/office-locations/{id}` | Get office |
| POST | `/api/admin/office-locations` | Create office |
| PUT | `/api/admin/office-locations/{id}` | Update office |
| DELETE | `/api/admin/office-locations/{id}` | Delete office |

**Office fields:** `officeName`, `address`, `latitude`, `longitude`, `radiusMeters` (50–300, default 100), `active`.

Updates invalidate Redis `office:employee:{id}` cache entries for affected employees.

### Attendance policies

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/policies` | List policies |
| GET | `/api/admin/policies/{id}` | Get policy |
| POST | `/api/admin/policies` | Create policy |
| PUT | `/api/admin/policies/{id}` | Update policy |
| DELETE | `/api/admin/policies/{id}` | Delete policy |

**Policy fields include:** `minimumWfoDaysPerWeek`, `standardCheckInTime`, `standardCheckOutTime`, `lateThresholdMinutes`, **`requiredWfoMinutes`** (default 180), `active`.

---

## Notifications

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications?page=&size=` | Paginated notifications for current user |
| GET | `/api/notifications/unread-count` | Unread count |
| POST | `/api/notifications/{id}/read` | Mark notification as read |

---

## Outliers

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/outliers?page=&size=&status=&severity=&type=` | Paginated outlier alerts with optional filters |

Manager team outliers also available at `GET /api/manager/outliers`.

### Outlier types

| Type | Typical trigger |
|------|-----------------|
| `FREQUENT_LATE_CHECK_IN` | Repeated late arrivals vs policy |
| `FREQUENT_ABSENCE` | High absence rate |
| `MISSING_CHECKOUT` | EOD system close without manual checkout |
| `REPEATED_SYSTEM_DAY_CLOSE` | Pattern of system day closes |
| `LOW_WFO_ATTENDANCE` | Low WFO rate based on final daily mode / office minutes |

---

## Health

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Application health (used by Docker healthcheck) |

---

## Attendance Behavior Summary (for API Consumers)

1. **Before check-in** — `POST /location-signal` with one location read:
   - Inside assigned office → auto WFO after stability period (~15s demo).
   - Outside → `requiresWfhConfirmation: true`; confirm via `POST /wfh-check-in`.
2. **Manual check-in** — `POST /check-in` classifies WFO (inside) or WFH (outside).
3. **WFO auto-checkout** — `POST /location-signal` from WFO watcher while app open; outside for grace period (~60s demo) triggers auto checkout. Applies to **all WFO sessions** (auto and manual).
4. **WFH sessions** — no continuous location signals; manual checkout or EOD close.
5. **Manual checkout** — `POST /check-out` closes any open session.
6. **Same-day re-check-in** — allowed after checkout when `currentSessionStatus` is not `OPEN`.
7. **Final daily mode** — WFO if `totalOfficeMinutes >= requiredWfoMinutes`; else WFH. No HYBRID.
8. **EOD close** — scheduler closes open sessions at 23:59:59; creates `SYSTEM_DAY_CLOSE` event.

See [attendance-flow.md](attendance-flow.md) and [architecture.md](architecture.md) for full rules.

---

## Related Documentation

- [attendance-flow.md](attendance-flow.md) — check-in/check-out flow
- [architecture.md](architecture.md) — outbox, Redis, schedulers
- [local-setup.md](local-setup.md) — testing APIs locally
