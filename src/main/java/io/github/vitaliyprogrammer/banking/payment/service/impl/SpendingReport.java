package io.github.vitaliyprogrammer.banking.payment.service.impl;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingCategory;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingResponseDto.CategoryTotal;
import io.github.vitaliyprogrammer.banking.payment.exception.InvalidPeriodException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Turns sums by payment type or provider category into the home screen's "where the money went". */
final class SpendingReport {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    /** A leap year: the home screen asks for at most one calendar year. */
    private static final long MAX_DAYS = 366;

    private SpendingReport() {
    }

    static void checkPeriod(LocalDate from, LocalDate to) {

        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new InvalidPeriodException(MAX_DAYS);
        }
    }

    /** The first moment of a Kyiv day. */
    static Instant startOf(LocalDate day) {
        return day.atStartOfDay(KYIV).toInstant();
    }

    /** The first moment after a Kyiv day, so the whole day is included. */
    static Instant endOf(LocalDate day) {
        return day.plusDays(1).atStartOfDay(KYIV).toInstant();
    }

    /** @param sumsByKind a payment type (TRANSFER, SAVINGS_DEPOSIT) or, for a bill, its provider's category (GAS) */
    static SpendingResponseDto build(LocalDate from, LocalDate to, Map<String, BigDecimal> sumsByKind) {

        Map<SpendingCategory, BigDecimal> totals = new EnumMap<>(SpendingCategory.class);
        sumsByKind.forEach((kind, amount) -> totals.merge(categoryOf(kind), amount, BigDecimal::add));

        List<CategoryTotal> categories = totals.entrySet().stream()
                .map(entry -> new CategoryTotal(entry.getKey(), money(entry.getValue())))
                .sorted(Comparator.comparing(CategoryTotal::amount).reversed())
                .toList();

        BigDecimal total = money(totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        return new SpendingResponseDto(from, to, Currency.UAH, total, categories);
    }

    private static SpendingCategory categoryOf(String kind) {

        return switch (kind) {
            case "TRANSFER", "CARD_TRANSFER" -> SpendingCategory.TRANSFERS;
            case "SAVINGS_DEPOSIT" -> SpendingCategory.SAVINGS;
            case "MOBILE" -> SpendingCategory.MOBILE;
            case "INTERNET" -> SpendingCategory.INTERNET;
            case "ELECTRICITY", "WATER", "GAS", "HEATING" -> SpendingCategory.UTILITIES;
            default -> throw new IllegalStateException("No spending category for " + kind);
        };
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
