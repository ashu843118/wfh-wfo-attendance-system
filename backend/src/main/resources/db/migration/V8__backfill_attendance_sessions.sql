-- Backfill attendance_sessions from historical check-in/check-out events (pre-V7 data)

INSERT INTO attendance_sessions (
    employee_id,
    team_id,
    attendance_date,
    attendance_record_id,
    session_mode,
    check_in_event_type,
    check_in_time,
    check_in_trigger_mode,
    check_out_time,
    check_out_event_type,
    auto_checkout_eligible,
    status,
    matched_office_location_id
)
SELECT
    ci.employee_id,
    ci.team_id,
    ci.attendance_date,
    ci.attendance_record_id,
    CASE
        WHEN ci.event_type = 'WFH_CONFIRMED_CHECK_IN' THEN 'WFH'
        WHEN ci.event_type = 'AUTO_CHECK_IN' THEN 'WFO'
        WHEN ci.matched_office_location_id IS NOT NULL THEN 'WFO'
        ELSE 'WFH'
    END AS session_mode,
    ci.event_type,
    ci.event_time,
    ci.trigger_mode,
    co.event_time,
    co.event_type,
    ci.event_type = 'AUTO_CHECK_IN',
    CASE
        WHEN co.id IS NULL THEN 'OPEN'
        WHEN co.event_type = 'SYSTEM_DAY_CLOSE' THEN 'SYSTEM_CLOSED'
        ELSE 'CLOSED'
    END AS status,
    ci.matched_office_location_id
FROM attendance_events ci
LEFT JOIN LATERAL (
    SELECT e.id, e.event_time, e.event_type
    FROM attendance_events e
    WHERE e.employee_id = ci.employee_id
      AND e.attendance_date = ci.attendance_date
      AND e.event_time >= ci.event_time
      AND e.id <> ci.id
      AND e.event_type IN ('AUTO_CHECK_OUT', 'MANUAL_CHECK_OUT', 'CHECK_OUT', 'SYSTEM_DAY_CLOSE')
      AND NOT EXISTS (
          SELECT 1
          FROM attendance_events mid
          WHERE mid.employee_id = ci.employee_id
            AND mid.attendance_date = ci.attendance_date
            AND mid.event_time > ci.event_time
            AND mid.event_time < e.event_time
            AND mid.event_type IN ('AUTO_CHECK_IN', 'MANUAL_CHECK_IN', 'WFH_CONFIRMED_CHECK_IN', 'CHECK_IN')
      )
    ORDER BY e.event_time, e.id
    LIMIT 1
) co ON TRUE
WHERE ci.event_type IN ('AUTO_CHECK_IN', 'MANUAL_CHECK_IN', 'WFH_CONFIRMED_CHECK_IN', 'CHECK_IN')
  AND ci.valid = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM attendance_sessions s
      WHERE s.employee_id = ci.employee_id
        AND s.attendance_date = ci.attendance_date
        AND s.check_in_time = ci.event_time
  );

UPDATE attendance_events e
SET attendance_session_id = s.id
FROM attendance_sessions s
WHERE e.employee_id = s.employee_id
  AND e.attendance_date = s.attendance_date
  AND e.event_time = s.check_in_time
  AND e.event_type IN ('AUTO_CHECK_IN', 'MANUAL_CHECK_IN', 'WFH_CONFIRMED_CHECK_IN', 'CHECK_IN')
  AND e.attendance_session_id IS NULL;

UPDATE attendance_events e
SET attendance_session_id = s.id
FROM attendance_sessions s
WHERE e.employee_id = s.employee_id
  AND e.attendance_date = s.attendance_date
  AND e.event_time = s.check_out_time
  AND e.event_type IN ('AUTO_CHECK_OUT', 'MANUAL_CHECK_OUT', 'CHECK_OUT', 'SYSTEM_DAY_CLOSE')
  AND s.check_out_time IS NOT NULL
  AND e.attendance_session_id IS NULL;
