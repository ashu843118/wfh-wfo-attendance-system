-- Immutable attendance event log + daily summary columns on attendance_records

CREATE TABLE attendance_events (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    team_id BIGINT REFERENCES teams(id),
    attendance_date DATE NOT NULL,
    attendance_record_id BIGINT REFERENCES attendance_records(id),
    event_type VARCHAR(30) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    geo_point geography(Point, 4326),
    source VARCHAR(50) NOT NULL DEFAULT 'PWA',
    trigger_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    valid BOOLEAN NOT NULL DEFAULT TRUE,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attendance_events_employee_date ON attendance_events(employee_id, attendance_date);
CREATE INDEX idx_attendance_events_record_id ON attendance_events(attendance_record_id);
CREATE INDEX idx_attendance_events_event_time ON attendance_events(event_time);
CREATE INDEX idx_attendance_events_type ON attendance_events(event_type);
CREATE INDEX idx_attendance_events_geo ON attendance_events USING GIST (geo_point);

COMMENT ON TABLE attendance_records IS 'Daily attendance summary - one row per employee per date';
COMMENT ON TABLE attendance_events IS 'Immutable attendance event history for audit and detailed views';

ALTER TABLE attendance_records RENAME COLUMN check_in_time TO first_check_in_time;
ALTER TABLE attendance_records RENAME COLUMN check_out_time TO final_check_out_time;

-- Backfill events from existing daily summaries
INSERT INTO attendance_events (
    employee_id, team_id, attendance_date, attendance_record_id,
    event_type, event_time, latitude, longitude, accuracy, geo_point, source, trigger_mode, valid
)
SELECT
    employee_id, team_id, attendance_date, id,
    'CHECK_IN', first_check_in_time, check_in_latitude, check_in_longitude, check_in_accuracy,
    check_in_geo_point, COALESCE(source, 'PWA'), 'MANUAL', TRUE
FROM attendance_records
WHERE first_check_in_time IS NOT NULL;

INSERT INTO attendance_events (
    employee_id, team_id, attendance_date, attendance_record_id,
    event_type, event_time, latitude, longitude, accuracy, geo_point, source, trigger_mode, valid
)
SELECT
    employee_id, team_id, attendance_date, id,
    'CHECK_OUT', final_check_out_time, check_out_latitude, check_out_longitude, check_out_accuracy,
    check_out_geo_point, COALESCE(source, 'PWA'), 'MANUAL', TRUE
FROM attendance_records
WHERE final_check_out_time IS NOT NULL;

UPDATE attendance_events e
SET attendance_record_id = r.id
FROM attendance_records r
WHERE e.attendance_record_id IS NULL
  AND e.employee_id = r.employee_id
  AND e.attendance_date = r.attendance_date;
