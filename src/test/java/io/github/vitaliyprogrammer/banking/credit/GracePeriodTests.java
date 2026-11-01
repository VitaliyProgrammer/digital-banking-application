package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.credit.service.impl.GracePeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/** The calendar borders of the interest-free period: month lengths, February, leap years, Kyiv midnight. */
class GracePeriodTests {

    @Test
    void spendingOnTheFirstOfDecemberGetsTheFull62Days() {
        LocalDate spentOn = LocalDate.of(2026, 12, 1);

        LocalDate until = GracePeriod.interestFreeUntil(spentOn);

        assertThat(until).isEqualTo(LocalDate.of(2027, 1, 31));
        // Counting both the day of spending and the last day: 1 Dec … 31 Jan.
        assertThat(ChronoUnit.DAYS.between(spentOn, until) + 1).isEqualTo(62);
    }

    @Test
    void spendingOnTheLastDayOfAMonthStillGetsTheWholeNextMonth() {
        assertThat(GracePeriod.interestFreeUntil(LocalDate.of(2026, 10, 31))).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void februaryEndsOnItsOwnLastDayInLeapAndCommonYears() {
        assertThat(GracePeriod.interestFreeUntil(LocalDate.of(2027, 1, 15))).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(GracePeriod.interestFreeUntil(LocalDate.of(2028, 1, 15))).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void theStatementClosesAtKyivMidnightNotUtcMidnight() {
        // 1 November 2026 00:00 in Kyiv (winter time, UTC+2) is still 31 October in UTC.
        assertThat(GracePeriod.lastStatementClosedAt(LocalDate.of(2026, 11, 17)).toInstant())
                .isEqualTo(Instant.parse("2026-10-31T22:00:00Z"));
        // 1 July (summer time, UTC+3).
        assertThat(GracePeriod.lastStatementClosedAt(LocalDate.of(2026, 7, 1)).toInstant())
                .isEqualTo(Instant.parse("2026-06-30T21:00:00Z"));
    }
}
