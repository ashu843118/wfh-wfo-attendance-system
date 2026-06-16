package com.wfhwfo.attendance.office.repository;

import com.wfhwfo.attendance.office.entity.OfficeLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficeLocationRepository extends JpaRepository<OfficeLocation, Long> {

    Page<OfficeLocation> findAllByOrderByOfficeNameAsc(Pageable pageable);

    List<OfficeLocation> findByActiveTrue();

    Optional<OfficeLocation> findFirstByOfficeName(String officeName);

    @Query(value = """
            SELECT o.id AS officeId,
                   o.office_name AS officeName,
                   ST_Distance(o.geo_point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distanceMeters
            FROM office_locations o
            WHERE o.active = true
              AND ST_DWithin(o.geo_point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, o.radius_meters)
            ORDER BY distanceMeters
            LIMIT 1
            """, nativeQuery = true)
    Optional<OfficeMatchProjection> findNearestOfficeWithinGeofence(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude);

    interface OfficeMatchProjection {
        Long getOfficeId();
        String getOfficeName();
        Double getDistanceMeters();
    }
}
