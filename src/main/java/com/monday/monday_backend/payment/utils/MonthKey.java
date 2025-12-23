package com.monday.monday_backend.payment.utils;

import java.time.*;

public final class MonthKey {
    private MonthKey() {}

    public static int currentUtcYYYYMM(Clock clock) {
        ZonedDateTime z = ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC);
        return z.getYear() * 100 + z.getMonthValue();
    }
}