package com.wfhwfo.attendance.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WorkingDayUtils {

    private WorkingDayUtils() {
    }

    public static long countWeekdaysInclusive(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return 1;
        }
        long count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return Math.max(count, 1);
    }
}
