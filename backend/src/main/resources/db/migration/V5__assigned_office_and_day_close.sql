-- One assigned office per employee (MVP); matched office on attendance records/events

ALTER TABLE employees
    ADD COLUMN assigned_office_location_id BIGINT REFERENCES office_locations(id);

ALTER TABLE attendance_records
    ADD COLUMN matched_office_location_id BIGINT REFERENCES office_locations(id);

ALTER TABLE attendance_events
    ADD COLUMN matched_office_location_id BIGINT REFERENCES office_locations(id);

-- Backfill assigned office for existing active employees (first active office)
UPDATE employees e
SET assigned_office_location_id = (
    SELECT o.id FROM office_locations o WHERE o.active = true ORDER BY o.id LIMIT 1
)
WHERE e.assigned_office_location_id IS NULL
  AND e.active = true;

CREATE INDEX idx_employees_assigned_office ON employees(assigned_office_location_id);

COMMENT ON COLUMN employees.assigned_office_location_id IS
    'MVP: one assigned office per employee. Future: employee_office_assignments table for multiple offices.';
