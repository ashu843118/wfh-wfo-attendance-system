-- Daily summary: optional total office time
ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS total_office_minutes INTEGER;

-- Enforce assigned office for attendance-tracked roles (MVP)
UPDATE employees e
SET assigned_office_location_id = (
    SELECT o.id FROM office_locations o WHERE o.active = true ORDER BY o.id LIMIT 1
)
WHERE e.assigned_office_location_id IS NULL
  AND e.active = true
  AND e.role IN ('EMPLOYEE', 'MANAGER');

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_assigned_office_for_attendance_roles
        CHECK (role NOT IN ('EMPLOYEE', 'MANAGER') OR assigned_office_location_id IS NOT NULL);
