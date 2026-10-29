package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Taking savings back is free: it is the client's own money. */
@Component
public class SavingsWithdrawalFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.SAVINGS_WITHDRAWAL;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
