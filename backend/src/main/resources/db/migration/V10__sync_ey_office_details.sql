-- Ensure EY Bengaluru demo office has correct coordinates, address, and geofence

UPDATE office_locations
SET
    address = 'Campus 1C, Ecospace Business Park, Bellandur, Outer Ring Road, Bengaluru, Karnataka 560103',
    latitude = 12.9262,
    longitude = 77.6811,
    geo_point = ST_SetSRID(ST_MakePoint(77.6811, 12.9262), 4326),
    radius_meters = 500,
    active = TRUE
WHERE office_name = 'EY Bengaluru - Ecospace';

UPDATE employees
SET assigned_office_location_id = (
    SELECT id FROM office_locations WHERE office_name = 'EY Bengaluru - Ecospace' LIMIT 1
)
WHERE email = 'employee@demo.com';
