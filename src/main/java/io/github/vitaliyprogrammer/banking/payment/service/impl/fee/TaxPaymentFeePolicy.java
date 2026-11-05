package io.github.vitaliyprogrammer.banking.payment.service.impl.fee;

import io.github.vitaliyprogrammer.banking.payment.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Paying taxes is free: a bank that charges for it would only push the client to pay elsewhere. */
@Component
public class TaxPaymentFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.TAX_PAYMENT;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
