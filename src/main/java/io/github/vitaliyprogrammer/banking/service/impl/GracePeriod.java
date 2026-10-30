package io.github.vitaliyprogrammer.banking.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The interest-free period of a credit line, by calendar months in Kyiv time. Everything spent in a month
 * is the statement of that month; it closes at midnight on the 1st and is interest-free until the end of the next
 * month. So spending on the 1st gets the longest period: 1 December → 31 January is 62 days.
 * Pure date arithmetic, no Spring and no database, so every border is a millisecond unit test.
 */
public final class GracePeriod {

    public static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private GracePeriod() {
    }

    /** The last day on which money spent on {@code spentOn} may be repaid without interest. */
    public static LocalDate interestFreeUntil(LocalDate spentOn) {

        return YearMonth.from(spentOn).plusMonths(1).atEndOfMonth();
    }

    /** The moment the last statement closed: midnight at the start of the current month. */
    public static ZonedDateTime lastStatementClosedAt(LocalDate today) {

        return today.withDayOfMonth(1).atStartOfDay(KYIV);
    }

    /** The statement of October closes at midnight on 1 November. */
    public static Instant statementClosedAt(YearMonth period) {

        return period.plusMonths(1).atDay(1).atStartOfDay(KYIV).toInstant();
    }

    /** The statement of October is due by 30 November... */
    public static LocalDate statementDueDate(YearMonth period) {

        return period.plusMonths(1).atEndOfMonth();
    }

    /** ...that is, repayments made before midnight on 1 December count. */
    public static Instant statementDueAt(YearMonth period) {

        return period.plusMonths(2).atDay(1).atStartOfDay(KYIV).toInstant();
    }
}
