package com.wfhwfo.attendance.office.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.util.GeoPointUtils;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.dto.PagedResponseMapper;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.office.dto.OfficeLocationRequest;
import com.wfhwfo.attendance.office.dto.OfficeLocationResponse;
import com.wfhwfo.attendance.office.entity.OfficeLocation;
import com.wfhwfo.attendance.office.repository.OfficeLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeLocationService {

    private static final String ACTIVE_OFFICES_CACHE_KEY = "office:locations:active";

    private final OfficeLocationRepository officeLocationRepository;
    private final CacheAdapter cacheAdapter;
    private final EmployeeOfficeCacheService employeeOfficeCacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.office-locations-ttl-seconds:300}")
    private long officeLocationsTtlSeconds;

    @Transactional(readOnly = true)
    public List<OfficeLocationResponse> getActiveOffices() {
        return cacheAdapter.get(ACTIVE_OFFICES_CACHE_KEY)
                .map(this::deserializeOffices)
                .orElseGet(this::loadAndCacheActiveOffices);
    }

    @Transactional(readOnly = true)
    public List<OfficeLocationResponse> getAll() {
        return officeLocationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<OfficeLocationResponse> list(Pageable pageable) {
        Page<OfficeLocation> page = officeLocationRepository.findAllByOrderByOfficeNameAsc(pageable);
        return PagedResponseMapper.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public OfficeLocationResponse getById(Long id) {
        return officeLocationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Office location not found", "OFFICE_NOT_FOUND"));
    }

    @Transactional
    public OfficeLocationResponse create(OfficeLocationRequest request) {
        OfficeLocation office = toEntity(new OfficeLocation(), request);
        OfficeLocation saved = officeLocationRepository.save(office);
        evictActiveOfficesCache();
        return toResponse(saved);
    }

    @Transactional
    public OfficeLocationResponse update(Long id, OfficeLocationRequest request) {
        OfficeLocation office = officeLocationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Office location not found", "OFFICE_NOT_FOUND"));
        OfficeLocation updated = toEntity(office, request);
        OfficeLocation saved = officeLocationRepository.save(updated);
        evictActiveOfficesCache();
        employeeOfficeCacheService.evictEmployeesForOffice(id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!officeLocationRepository.existsById(id)) {
            throw new BusinessException("Office location not found", "OFFICE_NOT_FOUND");
        }
        officeLocationRepository.deleteById(id);
        evictActiveOfficesCache();
        employeeOfficeCacheService.evictAllEmployeeOfficeCaches();
    }

    public void evictActiveOfficesCache() {
        cacheAdapter.evict(ACTIVE_OFFICES_CACHE_KEY);
    }

    private List<OfficeLocationResponse> loadAndCacheActiveOffices() {
        List<OfficeLocationResponse> offices = officeLocationRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
        try {
            cacheAdapter.put(
                    ACTIVE_OFFICES_CACHE_KEY,
                    objectMapper.writeValueAsString(offices),
                    Duration.ofSeconds(officeLocationsTtlSeconds)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to cache active office locations", ex);
        }
        return offices;
    }

    private List<OfficeLocationResponse> deserializeOffices(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return loadAndCacheActiveOffices();
        }
    }

    private OfficeLocation toEntity(OfficeLocation office, OfficeLocationRequest request) {
        office.setOfficeName(request.getOfficeName());
        office.setAddress(request.getAddress());
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        office.setGeoPoint(GeoPointUtils.createPoint(request.getLatitude(), request.getLongitude()));
        office.setRadiusMeters(request.getRadiusMeters());
        office.setActive(request.isActive());
        return office;
    }

    private OfficeLocationResponse toResponse(OfficeLocation office) {
        return OfficeLocationResponse.builder()
                .id(office.getId())
                .officeName(office.getOfficeName())
                .address(office.getAddress())
                .latitude(office.getLatitude())
                .longitude(office.getLongitude())
                .radiusMeters(office.getRadiusMeters())
                .active(office.isActive())
                .createdAt(office.getCreatedAt())
                .updatedAt(office.getUpdatedAt())
                .build();
    }
}
