package com.wfhwfo.attendance.attendance.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AutoAttendanceProperties.class)
public class AutoAttendanceConfig {
}
