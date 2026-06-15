# API Design

Base URL: `http://localhost:8080`

All authenticated endpoints require header: `Authorization: Bearer <JWT>`

Interactive documentation: **http://localhost:8080/swagger-ui/index.html**

## Standard Response Wrapper

```json
{
  "success": true,
  "message": "Operation completed",
  "data": { },
  "timestamp": "2026-06-14T10:05:01"
}
```

Paginated responses wrap content in `PagedResponse`:

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

## Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Demo login |
| GET | `/api/auth/me` | JWT | Current user profile |

**Login request:**
```json
{ "email": "employee@demo.com", "password": "password" }
```

**Login response data:**
```json
{
  "token": "eyJ...",
  "employeeId": 1,
  "email": "employee@demo.com",
  "name": "Ashutosh Kumar",
  "role": "EMPLOYEE",
  "teamId": 1,
  "managerId": 6
}
```

## Attendance

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/attendance/check-in` | Record check-in (async classification) |
| POST | `/api/attendance/check-out` | Record check-out |
| GET | `/api/attendance/me/today` | Today's attendance for current user |
| GET | `/api/attendance/me?from=&to=&page=&size=` | Paginated history |

**Check-in response:**
```json
{
  "attendanceId": 101,
  "status": "RECORDED",
  "processingStatus": "CLASSIFICATION_PENDING",
  "recordedAt": "2026-06-14T10:05:00"
}
```

## Manager

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/manager/dashboard-summary` | Team KPIs, charts, table |
| GET | `/api/manager/team-attendance` | Paginated team attendance |
| GET | `/api/manager/outliers` | Team outlier alerts |
| GET | `/api/manager/employees/{id}/attendance` | Employee attendance history |

## Leadership

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/leadership/dashboard` | Aggregated org dashboard |

## Admin

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/employees?page=&size=&search=&role=&teamId=&active=` | Paginated employee list |
| POST | `/api/admin/employees` | Create employee with temporary password |
| PUT | `/api/admin/employees/{id}` | Update employee |
| PATCH | `/api/admin/employees/{id}/status` | Activate/deactivate employee |
| GET | `/api/admin/managers` | Active managers for dropdown |
| GET | `/api/admin/teams` | Teams for dropdown |
| GET/POST/PUT | `/api/admin/office-locations` | Office geo-fence CRUD |
| GET/POST/PUT | `/api/admin/policies` | Attendance policy CRUD |

## Employee Dashboard

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/employee/dashboard-summary` | Personal KPIs and recent attendance |

## Notifications

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications` | Paginated notifications |
| GET | `/api/notifications/unread-count` | Unread count |
| PUT | `/api/notifications/{id}/read` | Mark as read |

## Health

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Application health |
