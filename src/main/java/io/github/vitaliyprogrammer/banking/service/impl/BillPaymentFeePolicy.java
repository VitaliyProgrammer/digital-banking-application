package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Utilities and mobile top-ups are free for the client, as in most Ukrainian mobile banks. */
@Component
public class BillPaymentFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.BILL_PAYMENT;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
