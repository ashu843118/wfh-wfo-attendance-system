package com.wfhwfo.attendance.office.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import com.wfhwfo.attendance.office.repository.EmployeeAssignedOfficeProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeOfficeCacheService {

    private static final String EMPLOYEE_OFFICE_KEY_PREFIX = "office:employee:";
    private static final String EMPLOYEE_OFFICE_PATTERN = "office:employee:*";

    private final CacheAdapter cacheAdapter;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.employee-office-ttl-seconds:900}")
    private long employeeOfficeTtlSeconds;

    @Transactional(readOnly = true)
    public EmployeeAssignedOfficeDto getAssignedOffice(Long employeeId) {
        String cacheKey = cacheKey(employeeId);
        Optional<String> cached = cacheAdapter.get(cacheKey);
        if (cached.isPresent()) {
            try {
                log.debug("Office cache hit employeeId={} key={}", employeeId, cacheKey);
                return objectMapper.readValue(cached.get(), EmployeeAssignedOfficeDto.class);
            } catch (JsonProcessingException ex) {
                log.warn("Failed to parse cached office for employee {}", employeeId);
                cacheAdapter.evict(cacheKey);
            }
        }

        log.debug("Office cache miss employeeId={} loading assigned office from DB", employeeId);
        EmployeeAssignedOfficeDto office = employeeRepository.findAssignedOfficeByEmployeeId(employeeId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(
                        "No assigned office configured for employee",
                        "EMPLOYEE_OFFICE_NOT_ASSIGNED"));

        cacheOffice(employeeId, office);
        return office;
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeAssignedOfficeDto> findAssignedOffice(Long employeeId) {
        try {
            return Optional.of(getAssignedOffice(employeeId));
        } catch (BusinessException ex) {
            if ("EMPLOYEE_OFFICE_NOT_ASSIGNED".equals(ex.getErrorCode())) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public void evictEmployeeOfficeCache(Long employeeId) {
        cacheAdapter.evict(cacheKey(employeeId));
        log.debug("Office cache evicted employeeId={} key={} reason=EMPLOYEE_OFFICE_UPDATED", employeeId, cacheKey(employeeId));
    }

    public void evictAllEmployeeOfficeCaches() {
        cacheAdapter.evictByPattern(EMPLOYEE_OFFICE_PATTERN);
        log.debug("Office cache evicted pattern={} reason=BULK_INVALIDATION", EMPLOYEE_OFFICE_PATTERN);
    }

    public void evictEmployeesForOffice(Long officeLocationId) {
        employeeRepository.findEmployeeIdsByAssignedOfficeLocationId(officeLocationId)
                .forEach(employeeId -> {
                    cacheAdapter.evict(cacheKey(employeeId));
                    log.debug(
                            "Office cache evicted employeeId={} key={} reason=OFFICE_LOCATION_UPDATED officeId={}",
                            employeeId,
                            cacheKey(employeeId),
                            officeLocationId);
                });
    }

    private void cacheOffice(Long employeeId, EmployeeAssignedOfficeDto office) {
        try {
            cacheAdapter.put(
                    cacheKey(employeeId),
                    objectMapper.writeValueAsString(office),
                    Duration.ofSeconds(employeeOfficeTtlSeconds));
            log.debug("Office cache put employeeId={} key={} officeId={}", employeeId, cacheKey(employeeId), office.getOfficeLocationId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to cache assigned office", ex);
        }
    }

    private EmployeeAssignedOfficeDto toDto(EmployeeAssignedOfficeProjection projection) {
        return EmployeeAssignedOfficeDto.builder()
                .officeLocationId(projection.getOfficeLocationId())
                .officeName(projection.getOfficeName())
                .address(projection.getAddress())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .radiusMeters(projection.getRadiusMeters())
                .active(Boolean.TRUE.equals(projection.getActive()))
                .build();
    }

    private String cacheKey(Long employeeId) {
        return EMPLOYEE_OFFICE_KEY_PREFIX + employeeId;
    }
}
