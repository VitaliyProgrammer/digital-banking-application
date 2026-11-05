package io.github.vitaliyprogrammer.banking.business.dto.response;

import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A sole proprietor's profile, their two accounts and the tax of the current quarter. */
public record BusinessResponseDto(
        UUID id,
        String name,
        String taxId,
        List<String> kvedCodes,
        TaxGroup taxGroup,
        boolean vatPayer,
        LocalDate registeredOn,
        UUID businessAccountId,
        String businessAccountNumber,
        BigDecimal businessBalance,
        UUID taxReserveAccountId,
        /* Whether the bank pays the tax on its due date by itself. */
        boolean autoPayTax,
        /* What is in the tax jar right now. */
        BigDecimal taxReserve,
        /* Group 3: the share of every income that goes into the tax jar; zero for groups 1 and 2. */
        BigDecimal taxRatePercent,
        /* Groups 1 and 2: the fixed tax per month; zero for group 3. */
        BigDecimal monthlyTax,
        /* E.g. "2026-Q4", Kyiv time. */
        String quarter,
        BigDecimal quarterIncome,
        /* Group 3: put aside this quarter; groups 1 and 2: three months of the fixed tax. */
        BigDecimal quarterTax
) {
}
