-- Persisted attendance sessions (multiple per day) and daily-mode policy threshold

CREATE TABLE attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    team_id BIGINT REFERENCES teams(id),
    attendance_date DATE NOT NULL,
    attendance_record_id BIGINT REFERENCES attendance_records(id),
    session_mode VARCHAR(10) NOT NULL,
    check_in_event_type VARCHAR(30) NOT NULL,
    check_in_time TIMESTAMP NOT NULL,
    check_in_trigger_mode VARCHAR(20) NOT NULL,
    check_out_time TIMESTAMP,
    check_out_event_type VARCHAR(30),
    auto_checkout_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    matched_office_location_id BIGINT REFERENCES office_locations(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attendance_sessions_employee_date ON attendance_sessions(employee_id, attendance_date);
CREATE INDEX idx_attendance_sessions_open ON attendance_sessions(employee_id, attendance_date, status);

ALTER TABLE attendance_events
    ADD COLUMN IF NOT EXISTS attendance_session_id BIGINT REFERENCES attendance_sessions(id);

ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS current_session_status VARCHAR(20) NOT NULL DEFAULT 'NONE';

ALTER TABLE attendance_policies
    ADD COLUMN IF NOT EXISTS required_wfo_minutes INTEGER NOT NULL DEFAULT 180;

UPDATE attendance_policies SET required_wfo_minutes = 180 WHERE required_wfo_minutes IS NULL;
