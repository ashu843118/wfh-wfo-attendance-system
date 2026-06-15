-- Remove legacy SQL seed (pre-hybrid seeder) so Java demo seeder can populate fresh data.
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM employees) > 0 AND (SELECT COUNT(*) FROM employees) < 90 THEN
        TRUNCATE TABLE notifications, attendance_outliers, attendance_records,
            attendance_policies, office_locations, employees, teams RESTART IDENTITY CASCADE;
    END IF;
END $$;
