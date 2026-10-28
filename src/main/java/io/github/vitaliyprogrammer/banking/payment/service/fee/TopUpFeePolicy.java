package io.github.vitaliyprogrammer.banking.payment.service.fee;

import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Top-ups are free: the bank wants money to come in. */
@Component
public class TopUpFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.TOP_UP;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
