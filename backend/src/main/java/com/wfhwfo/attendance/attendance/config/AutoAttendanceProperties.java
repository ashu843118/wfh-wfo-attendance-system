package com.wfhwfo.attendance.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.attendance.auto")
public class AutoAttendanceProperties {

    /**
     * Demo/assignment default: 15 seconds. Production recommendation: 2–5 minutes (120–300 seconds).
     */
    private int checkInStableSeconds = 15;

    /**
     * Demo/assignment default: 60 seconds. Production recommendation: 15–30 minutes (900–1800 seconds).
     */
    private int checkoutGraceSeconds = 60;
}
