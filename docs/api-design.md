# API Design

Base URL: `http://localhost:8080`

Interactive documentation: **http://localhost:8080/swagger-ui/index.html**

All authenticated endpoints require:

```
Authorization: Bearer <JWT>
```

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
| GET | `/api/attendance/me/today` | Today's daily summary + session status for current employee |
| GET | `/api/attendance/me?from=&to=&page=&size=` | Paginated daily attendance history |
| POST | `/api/attendance/location-signal` | Process foreground location signal (auto WFO, WFH prompt, auto checkout) |
| POST | `/api/attendance/check-in` | Manual check-in; classifies WFO/WFH from geofence |
| POST | `/api/attendance/wfh-check-in` | Confirm WFH when outside assigned office |
| POST | `/api/attendance/dismiss-wfh-prompt` | Dismiss WFH confirmation prompt for today |
| POST | `/api/attendance/check-out` | Manual check-out; closes active session |
| GET | `/api/attendance/me/events?from=&to=&page=&size=` | Paginated event audit history |
| GET | `/api/attendance/me/events/{date}` | All events for a specific date (YYYY-MM-DD) |
| POST | `/api/attendance/events/auto` | Low-level auto geofence enter/exit event (used internally by location flow) |

> **Note:** There is no `/api/attendance/today` alias. Use **`/api/attendance/me/today`**.

### Location signal

**Request body (`LocationPayload`):**

```json
{
  "latitude": 18.5912,
  "longitude": 73.7389,
  "accuracy": 10,
  "timestamp": "2026-06-15T09:30:00"
}
```

**Response `data` highlights:**

```json
{
  "trackingState": "WFH_CONFIRMATION_REQUIRED",
  "insideOffice": false,
  "assignedOfficeName": "Pune Tech Park",
  "distanceFromOfficeMeters": 14703.05,
  "requiresWfhConfirmation": true,
  "checkInStableSecondsRemaining": null,
  "graceSecondsRemaining": null,
  "userMessage": "You are inside office geofence. Auto check-in recorded as WFO.",
  "todaySummary": { },
  "actionTaken": { }
}
```

`trackingState` values include: `WAITING_FOR_PERMISSION`, `LOCATION_PERMISSION_DENIED`, `AUTO_CHECKIN_PENDING`, `CHECKED_IN_WFO`, `CHECKED_IN_WFH`, `WFH_CONFIRMATION_REQUIRED`, `NOT_CHECKED_IN`, `AUTO_CHECKOUT_PENDING`, `CHECKED_OUT`, `MISSING_CHECKOUT`, `SYSTEM_CLOSED`.

### Manual check-in / check-out

**Check-in request:**

```json
{
  "location": {
    "latitude": 18.5912,
    "longitude": 73.7389,
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

Key fields evaluators should understand:

| Field | Meaning |
|-------|---------|
| `attendanceMode` | Final daily mode: **WFO** or **WFH** (no HYBRID in MVP) |
| `currentSessionStatus` | `NONE`, `OPEN`, or `CLOSED` |
| `totalOfficeMinutes` | Sum of WFO session durations for the day |
| `checkInTime` | Earliest check-in (`first_check_in_time`) |
| `checkOutTime` | Latest checkout (`final_check_out_time`) |
| `status` | e.g. `CHECKED_IN`, `CHECKED_OUT`, `MISSING_CHECKOUT` |
| `processingStatus` | `CLASSIFICATION_PENDING` or `COMPLETED` |

---

## Employee Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/employee/dashboard-summary` | Personal KPIs, recent daily records, 30-day WFO/WFH trend |

Uses **`attendance_records`** daily summaries, not raw events.

---

## Manager Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/manager/dashboard-summary` | Team KPIs, charts, recent attendance |
| GET | `/api/manager/team-attendance?page=&size=&date=` | Paginated team daily records |
| GET | `/api/manager/outliers?page=&size=` | Team outlier alerts |
| GET | `/api/manager/employees/{employeeId}/attendance?from=&to=&page=&size=` | Individual employee history |

WFO/WFH counts use **final daily `attendance_mode`** from `attendance_records`.

---

## Leadership Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/leadership/dashboard` | Organization-level aggregated trends and KPIs |

Uses aggregated **`attendance_records`** final daily modes.

---

## Admin

Base path: `/api/admin`

### Employees

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/employees?page=&size=&search=&role=&teamId=&active=` | Paginated employee list |
| POST | `/api/admin/employees` | Create employee (includes `assignedOfficeLocationId`, temporary password) |
| PUT | `/api/admin/employees/{id}` | Update employee (including assigned office) |
| PATCH | `/api/admin/employees/{id}/status` | Activate/deactivate |
| GET | `/api/admin/managers` | Active managers for dropdown |
| GET | `/api/admin/teams` | Teams for dropdown |

**Create/update employee** must include **`assignedOfficeLocationId`** for EMPLOYEE and MANAGER roles.

### Office locations

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/office-locations` | List offices |
| GET | `/api/admin/office-locations/{id}` | Get office |
| POST | `/api/admin/office-locations` | Create office |
| PUT | `/api/admin/office-locations/{id}` | Update office |
| DELETE | `/api/admin/office-locations/{id}` | Delete office |

**Office fields:** `officeName`, `address`, `latitude`, `longitude`, `radiusMeters`, `active`.

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

## Outliers (via manager dashboard)

Outlier types detected asynchronously:

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

## Attendance behavior summary (for API consumers)

1. **Auto WFO** — `POST /location-signal` while inside assigned office → creates `AUTO_CHECK_IN` session after stability period.
2. **WFH prompt** — `POST /location-signal` while outside → `requiresWfhConfirmation: true`; confirm via `POST /wfh-check-in`.
3. **Manual check-in** — `POST /check-in` classifies WFO/WFH from geofence.
4. **Auto checkout** — only for sessions with `autoCheckoutEligible=true` (auto WFO); triggered via continued outside `location-signal`s after grace period.
5. **Manual checkout** — `POST /check-out` closes any open session.
6. **Same-day re-check-in** — allowed after checkout when `currentSessionStatus` is not `OPEN`.
7. **Final daily mode** — read from `GET /me/today` or history; based on `totalOfficeMinutes` vs `requiredWfoMinutes`.

See [architecture.md](architecture.md) and [assumptions.md](assumptions.md) for full rules.
