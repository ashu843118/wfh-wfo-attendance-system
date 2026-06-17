-- Per check-in event WFO/WFH session mode and distance from assigned office

ALTER TABLE attendance_events
    ADD COLUMN IF NOT EXISTS session_mode VARCHAR(10);

ALTER TABLE attendance_events
    ADD COLUMN IF NOT EXISTS distance_from_office_meters DOUBLE PRECISION;

UPDATE attendance_events e
SET session_mode = s.session_mode
FROM attendance_sessions s
WHERE e.attendance_session_id = s.id
  AND e.session_mode IS NULL
  AND e.event_type IN ('CHECK_IN', 'MANUAL_CHECK_IN', 'AUTO_CHECK_IN', 'WFH_CONFIRMED_CHECK_IN');

UPDATE attendance_events
SET session_mode = CASE
    WHEN event_type = 'WFH_CONFIRMED_CHECK_IN' THEN 'WFH'
    WHEN event_type = 'AUTO_CHECK_IN' THEN 'WFO'
    WHEN matched_office_location_id IS NOT NULL THEN 'WFO'
    ELSE 'WFH'
END
WHERE session_mode IS NULL
  AND event_type IN ('CHECK_IN', 'MANUAL_CHECK_IN', 'AUTO_CHECK_IN', 'WFH_CONFIRMED_CHECK_IN');

COMMENT ON COLUMN attendance_events.session_mode IS
    'WFO or WFH for this check-in event; decided by backend geofence validation during check-in';
