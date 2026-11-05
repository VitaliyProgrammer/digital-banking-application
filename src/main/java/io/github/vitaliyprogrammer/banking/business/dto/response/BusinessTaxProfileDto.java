package io.github.vitaliyprogrammer.banking.business.dto.response;

import io.github.vitaliyprogrammer.banking.business.entity.status.TaxGroup;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** For the tax module: what it needs to know about a sole proprietor to work out and pay their tax. */
public record BusinessTaxProfileDto(
        UUID businessId,
        UUID ownerId,
        TaxGroup taxGroup,
        /* Groups 1 and 2: the fixed tax per month; zero for group 3. */
        BigDecimal monthlyTax,
        UUID businessAccountId,
        UUID taxReserveAccountId,
        boolean autoPayTax,
        /* Taxes are owed from the month (or quarter) the business was opened in the bank. */
        Instant openedAt
) {
}
