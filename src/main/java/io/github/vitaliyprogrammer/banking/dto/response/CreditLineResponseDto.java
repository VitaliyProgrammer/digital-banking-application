package io.github.vitaliyprogrammer.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A client's credit line: how much is owed, how much may still be spent, and what to repay to pay no interest. */
public record CreditLineResponseDto(
        UUID accountId,
        String number,
        BigDecimal creditLimit,
        /* What may still be spent: the limit minus the debt, plus any own money on the account. Never below zero. */
        BigDecimal available,
        /* Zero when nothing is owed. May exceed the limit after interest and fees. */
        BigDecimal debt,
        /* The debt when last month's statement closed (midnight on the 1st). */
        BigDecimal statementDebt,
        /* What is still to repay of that statement by statementInterestFreeUntil to keep it free of interest. */
        BigDecimal leftToRepayInterestFree,
        /* The least to repay of that statement by the same day to avoid a late fee. */
        BigDecimal minimumPayment,
        /* What is still to repay to reach the minimum payment. */
        BigDecimal leftForMinimumPayment,
        LocalDate statementInterestFreeUntil,
        /* Money spent this month is interest-free until this day: the end of next month. */
        LocalDate newSpendingInterestFreeUntil
) {
}
