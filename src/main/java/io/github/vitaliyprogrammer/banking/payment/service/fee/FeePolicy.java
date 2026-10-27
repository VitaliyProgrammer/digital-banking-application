package io.github.vitaliyprogrammer.banking.payment.service.fee;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;

/**
 * Strategy: one implementation per payment type. A new tariff is a new class;
 * {@link FeeCalculator} picks it up automatically and existing tariffs stay untouched.
 */
public interface FeePolicy {

    PaymentType paymentType();

    /** Fee in the payment's currency, already rounded to 2 decimal places. */
    BigDecimal calculate(BigDecimal amount, Currency currency);
}
