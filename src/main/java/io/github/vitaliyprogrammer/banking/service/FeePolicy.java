package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import java.math.BigDecimal;

/**
 * Strategy: one implementation per payment type. A new tariff is a new class;
 * {@link FeeCalculator} picks it up automatically and existing tariffs stay untouched.
 */
public interface FeePolicy {

    PaymentType paymentType();

    /** Fee in the payment's currency, already rounded to 2 decimal places. */
    BigDecimal calculate(FeeRequest request);
}
