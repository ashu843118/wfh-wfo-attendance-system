package com.wfhwfo.attendance.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.attendance.day-close")
public class DayCloseProperties {

    private String defaultCloseTime = "23:59:59";
    private String cron = "0 5 0 * * *";
}
