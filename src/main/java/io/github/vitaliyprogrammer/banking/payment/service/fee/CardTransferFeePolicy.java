package io.github.vitaliyprogrammer.banking.payment.service.fee;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Card-to-card transfer: the rate depends on the sender's card type. A free tariff has no minimum fee;
 * a paid one is never below the bank's minimum per currency.
 */
@Component
public class CardTransferFeePolicy implements FeePolicy {

    private static final Map<CardType, BigDecimal> RATE_BY_CARD_TYPE = new EnumMap<>(Map.of(
            CardType.PAYROLL, new BigDecimal("0.005"),
            CardType.STUDENT, BigDecimal.ZERO,
            CardType.VIRTUAL, new BigDecimal("0.010"),
            CardType.CREDIT, new BigDecimal("0.020")));

    /** Fails on startup if a new card type was added without a tariff, instead of failing on the first payment. */
    public CardTransferFeePolicy() {

        for (CardType type : CardType.values()) {
            if (!RATE_BY_CARD_TYPE.containsKey(type)) {
                throw new IllegalStateException("No card transfer rate for card type " + type);
            }
        }
    }

    @Override
    public PaymentType paymentType() {

        return PaymentType.CARD_TRANSFER;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        BigDecimal rate = RATE_BY_CARD_TYPE.get(request.cardType());

        if (rate.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal percentageFee = request.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        return percentageFee.max(TransferFeePolicy.MINIMUM_FEE.get(request.currency()));
    }
}
