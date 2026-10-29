package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.service.FeePolicy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** No separate fee: the bank earns on the difference between its buy and sell rates. */
@Component
public class ExchangeFeePolicy implements FeePolicy {

    @Override
    public PaymentType paymentType() {

        return PaymentType.EXCHANGE;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        return BigDecimal.ZERO.setScale(2);
    }
}
