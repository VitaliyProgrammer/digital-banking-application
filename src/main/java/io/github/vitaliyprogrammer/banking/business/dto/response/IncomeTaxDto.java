package io.github.vitaliyprogrammer.banking.business.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * For the payment module: an incoming payment is a sole proprietor's income, and this much of it goes into their
 * tax jar. The tax may be zero (groups 1 and 2 pay a fixed amount), and the income is still recorded.
 */
public record IncomeTaxDto(
        UUID businessId,
        UUID taxReserveAccountId,
        BigDecimal tax
) {
}
