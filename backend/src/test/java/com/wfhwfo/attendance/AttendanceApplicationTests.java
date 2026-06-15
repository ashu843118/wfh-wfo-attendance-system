package com.wfhwfo.attendance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceApplicationTests {

    @Test
    void applicationClassLoads() {
        assertThat(AttendanceApplication.class).isNotNull();
    }
}
