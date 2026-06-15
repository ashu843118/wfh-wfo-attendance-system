-- PostGIS extension and schema
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    team_id BIGINT REFERENCES teams(id),
    manager_id BIGINT REFERENCES employees(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_team_id ON employees(team_id);
CREATE INDEX idx_employees_manager_id ON employees(manager_id);
CREATE INDEX idx_employees_email ON employees(email);

CREATE TABLE office_locations (
    id BIGSERIAL PRIMARY KEY,
    office_name VARCHAR(150) NOT NULL,
    address VARCHAR(500),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geo_point geography(Point, 4326) NOT NULL,
    radius_meters INTEGER NOT NULL DEFAULT 500,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_office_locations_geo_point ON office_locations USING GIST (geo_point);

CREATE TABLE attendance_policies (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    minimum_wfo_days_per_week INTEGER NOT NULL DEFAULT 2,
    standard_check_in_time TIME NOT NULL DEFAULT '09:30:00',
    standard_check_out_time TIME NOT NULL DEFAULT '18:30:00',
    late_threshold_minutes INTEGER NOT NULL DEFAULT 15,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    team_id BIGINT REFERENCES teams(id),
    attendance_date DATE NOT NULL,
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    check_in_latitude DOUBLE PRECISION,
    check_in_longitude DOUBLE PRECISION,
    check_in_accuracy DOUBLE PRECISION,
    check_in_geo_point geography(Point, 4326),
    check_out_latitude DOUBLE PRECISION,
    check_out_longitude DOUBLE PRECISION,
    check_out_accuracy DOUBLE PRECISION,
    check_out_geo_point geography(Point, 4326),
    attendance_mode VARCHAR(20) DEFAULT 'UNKNOWN',
    status VARCHAR(30) NOT NULL DEFAULT 'RECORDED',
    processing_status VARCHAR(30) NOT NULL DEFAULT 'CLASSIFICATION_PENDING',
    late BOOLEAN DEFAULT FALSE,
    distance_from_office_meters DOUBLE PRECISION,
    source VARCHAR(50) DEFAULT 'PWA',
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_attendance_employee_date UNIQUE (employee_id, attendance_date)
);

CREATE INDEX idx_attendance_employee_id ON attendance_records(employee_id);
CREATE INDEX idx_attendance_team_id ON attendance_records(team_id);
CREATE INDEX idx_attendance_date ON attendance_records(attendance_date);
CREATE INDEX idx_attendance_status ON attendance_records(status);
CREATE INDEX idx_attendance_processing_status ON attendance_records(processing_status);
CREATE INDEX idx_attendance_mode ON attendance_records(attendance_mode);
CREATE INDEX idx_attendance_check_in_geo ON attendance_records USING GIST (check_in_geo_point);

CREATE TABLE attendance_outliers (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    team_id BIGINT REFERENCES teams(id),
    attendance_record_id BIGINT REFERENCES attendance_records(id),
    outlier_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    detected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outliers_employee_id ON attendance_outliers(employee_id);
CREATE INDEX idx_outliers_team_id ON attendance_outliers(team_id);
CREATE INDEX idx_outliers_status ON attendance_outliers(status);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_employee_id BIGINT NOT NULL REFERENCES employees(id),
    related_employee_id BIGINT REFERENCES employees(id),
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_employee_id);
CREATE INDEX idx_notifications_read ON notifications(read);

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_employee_id BIGINT REFERENCES employees(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
