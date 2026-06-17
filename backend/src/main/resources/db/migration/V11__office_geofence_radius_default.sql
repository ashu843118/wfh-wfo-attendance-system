-- Realistic office geofence radius: default 100 m, EY demo office 100 m

ALTER TABLE office_locations
    ALTER COLUMN radius_meters SET DEFAULT 100;

UPDATE office_locations
SET radius_meters = 100
WHERE office_name = 'EY Bengaluru - Ecospace';
