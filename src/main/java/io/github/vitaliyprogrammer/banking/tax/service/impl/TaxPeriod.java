package io.github.vitaliyprogrammer.banking.tax.service.impl;

import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One tax period of a sole proprietor and its due date, by the Tax Code:
 * groups 1 and 2 pay for a month in advance, by the 20th of that month;
 * group 3 pays for a quarter within 50 days after it ends (40 for the declaration, 10 more to pay).
 * Pure value: no database, no clock.
 */
public record TaxPeriod(String code, Instant from, Instant to, LocalDate dueDate) {

    public static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static final int MONTHLY_DUE_DAY = 20;

    private static final int QUARTERLY_DUE_DAYS = 50;

    private static final Pattern MONTH = Pattern.compile("(\\d{4})-(\\d{2})");

    private static final Pattern QUARTER = Pattern.compile("(\\d{4})-Q([1-4])");

    private static final String[] ROMAN = {"I", "II", "III", "IV"};

    public static TaxPeriod month(YearMonth month) {

        return new TaxPeriod(month.toString(),
                month.atDay(1).atStartOfDay(KYIV).toInstant(),
                month.plusMonths(1).atDay(1).atStartOfDay(KYIV).toInstant(),
                month.atDay(MONTHLY_DUE_DAY));
    }

    public static TaxPeriod quarter(int year, int quarter) {

        YearMonth first = YearMonth.of(year, (quarter - 1) * 3 + 1);
        YearMonth next = first.plusMonths(3);
        return new TaxPeriod(year + "-Q" + quarter,
                first.atDay(1).atStartOfDay(KYIV).toInstant(),
                next.atDay(1).atStartOfDay(KYIV).toInstant(),
                first.plusMonths(2).atEndOfMonth().plusDays(QUARTERLY_DUE_DAYS));
    }

    /** "2026-11" for a month, "2026-Q4" for a quarter; anything else is not a period. */
    public static TaxPeriod parse(String code) {

        Matcher quarter = QUARTER.matcher(code);
        if (quarter.matches()) {
            return quarter(Integer.parseInt(quarter.group(1)), Integer.parseInt(quarter.group(2)));
        }
        Matcher month = MONTH.matcher(code);
        if (month.matches()) {
            return month(YearMonth.of(Integer.parseInt(month.group(1)), Integer.parseInt(month.group(2))));
        }
        throw new IllegalArgumentException("Not a tax period: " + code);
    }

    /** Every period from the one the business was opened in up to the one that holds today, oldest first. */
    public static List<TaxPeriod> since(TaxGroup group, LocalDate opened, LocalDate today) {

        List<TaxPeriod> periods = new ArrayList<>();
        if (group.paysShareOfIncome()) {
            int year = opened.getYear();
            int quarter = quarterOf(opened);
            while (year < today.getYear() || (year == today.getYear() && quarter <= quarterOf(today))) {
                periods.add(quarter(year, quarter));
                quarter++;
                if (quarter > 4) {
                    quarter = 1;
                    year++;
                }
            }
        } else {
            for (YearMonth month = YearMonth.from(opened); !month.isAfter(YearMonth.from(today)); month = month.plusMonths(1)) {
                periods.add(month(month));
            }
        }
        return periods;
    }

    public boolean isQuarter() {

        return code.contains("Q");
    }

    /** Whether the period is still going on: group 3 tax of the current quarter is still growing. */
    public boolean isOpenOn(LocalDate today) {

        return today.atStartOfDay(KYIV).toInstant().isBefore(to);
    }

    /** For people: "11.2026" or "IV квартал 2026". */
    public String label() {

        Matcher quarter = QUARTER.matcher(code);
        if (quarter.matches()) {
            return ROMAN[Integer.parseInt(quarter.group(2)) - 1] + " квартал " + quarter.group(1);
        }
        return code.substring(5) + "." + code.substring(0, 4);
    }

    private static int quarterOf(LocalDate date) {

        return (date.getMonthValue() - 1) / 3 + 1;
    }
}
