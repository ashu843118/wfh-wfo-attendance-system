package com.wfhwfo.attendance.demo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.demo.seed")
public class DemoDataProperties {

    private boolean enabled = false;
    private int minEmployeesToSkip = 90;
    private int attendanceDays = 30;
    private long randomSeed = 42L;
    private int batchSize = 200;
}
