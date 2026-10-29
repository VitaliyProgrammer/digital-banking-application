package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Putting money aside is free: the bank wants clients to save. */
@Component
public class SavingsDepositFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.SAVINGS_DEPOSIT;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
