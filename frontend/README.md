# WFH/WFO Attendance — Frontend

React + Vite PWA for the WFH/WFO Attendance Tracking App.

## Quick start

With the full stack running via Docker Compose (see root [README.md](../README.md)):

- App: http://localhost:3000
- Demo login: `employee@demo.com` / `password`

## Local development

```bash
npm install
npm run dev
```

Create `.env.local`:

```
VITE_API_BASE_URL=http://localhost:8080
```

Start backend separately (see [docs/local-setup.md](../docs/local-setup.md)).

## Build

```bash
npm run build
```

## Employee dashboard behavior

- Requests browser location automatically on login (no toggle).
- Auto WFO check-in when inside assigned office geofence.
- WFH confirmation prompt when outside (never silent WFH).
- Manual check-in/out always available when session state allows.

See root [README.md](../README.md) for full product rules.
