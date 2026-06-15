package com.wfhwfo.attendance.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingDayUtilsTest {

    @Test
    void countsWeekdaysOnly() {
        LocalDate from = LocalDate.of(2026, 6, 9);  // Monday
        LocalDate to = LocalDate.of(2026, 6, 15);   // Sunday

        assertThat(WorkingDayUtils.countWeekdaysInclusive(from, to)).isEqualTo(5);
    }
}
