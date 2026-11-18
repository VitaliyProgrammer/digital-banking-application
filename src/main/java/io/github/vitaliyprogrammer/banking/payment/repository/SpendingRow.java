package io.github.vitaliyprogrammer.banking.payment.repository;

import java.math.BigDecimal;

/** One row of {@link PaymentRepository#sumSpending}: a payment type or a provider category, and how much went to it. */
public interface SpendingRow {

    String getKind();

    BigDecimal getAmount();
}
