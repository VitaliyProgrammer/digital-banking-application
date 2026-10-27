package io.github.vitaliyprogrammer.banking.payment.service.fee;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Transfer to another client: 0.5% of the amount, but not less than a minimum per currency. */
@Component
public class TransferFeePolicy implements FeePolicy {

    private static final BigDecimal RATE = new BigDecimal("0.005");

    private static final Map<Currency, BigDecimal> MINIMUM_FEE = Map.of(
            Currency.UAH, new BigDecimal("5.00"),
            Currency.USD, new BigDecimal("0.20"),
            Currency.EUR, new BigDecimal("0.20"));

    @Override
    public PaymentType paymentType() {

        return PaymentType.TRANSFER;
    }

    /** HALF_UP, like everyday commercial rounding: 0.125 becomes 0.13. */
    @Override
    public BigDecimal calculate(BigDecimal amount, Currency currency) {

        BigDecimal percentageFee = amount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);

        return percentageFee.max(MINIMUM_FEE.get(currency));
    }
}
