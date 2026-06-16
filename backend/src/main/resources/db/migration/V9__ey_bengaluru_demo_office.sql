-- EY Bengaluru demo office and demo employee assignment (existing databases)

INSERT INTO office_locations (office_name, address, latitude, longitude, geo_point, radius_meters, active)
SELECT
    'EY Bengaluru - Ecospace',
    'Campus 1C, Ecospace Business Park, Bellandur, Outer Ring Road, Bengaluru, Karnataka 560103',
    12.9262,
    77.6811,
    ST_SetSRID(ST_MakePoint(77.6811, 12.9262), 4326),
    500,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM office_locations WHERE office_name = 'EY Bengaluru - Ecospace'
);

UPDATE employees
SET assigned_office_location_id = (
    SELECT id FROM office_locations WHERE office_name = 'EY Bengaluru - Ecospace' LIMIT 1
)
WHERE email = 'employee@demo.com';
