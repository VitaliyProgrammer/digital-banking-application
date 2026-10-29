package io.github.vitaliyprogrammer.banking.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Projection: what one account earned in cashback in a month. Spring Data fills it from the query aliases. */
public interface CashbackAccountTotal {

    UUID getAccountId();

    BigDecimal getTotal();
}
