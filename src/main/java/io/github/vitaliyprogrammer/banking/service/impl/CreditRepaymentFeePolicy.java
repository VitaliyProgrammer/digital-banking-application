package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Paying a debt back is free: a fee here would only push clients towards interest and overdue debt. */
@Component
public class CreditRepaymentFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.CREDIT_REPAYMENT;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
