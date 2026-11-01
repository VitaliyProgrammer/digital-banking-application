package io.github.vitaliyprogrammer.banking.payment.service.impl.fee;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import io.github.vitaliyprogrammer.banking.payment.service.FeePolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Bills: a small percentage that depends on what is paid, never below a minimum.
 * Utilities and internet 0.5%, mobile top-ups 1% (they earn 5% cashback, so the client still ends up ahead).
 */
@Component
public class BillPaymentFeePolicy implements FeePolicy {

    private static final BigDecimal UTILITIES_RATE = new BigDecimal("0.005");

    private static final Map<ProviderCategory, BigDecimal> RATE_BY_CATEGORY = new EnumMap<>(Map.of(
            ProviderCategory.ELECTRICITY, UTILITIES_RATE,
            ProviderCategory.WATER, UTILITIES_RATE,
            ProviderCategory.GAS, UTILITIES_RATE,
            ProviderCategory.HEATING, UTILITIES_RATE,
            ProviderCategory.INTERNET, UTILITIES_RATE,
            ProviderCategory.MOBILE, new BigDecimal("0.010")));

    private static final Map<Currency, BigDecimal> MINIMUM_FEE = new EnumMap<>(Map.of(
            Currency.UAH, new BigDecimal("1.00"),
            Currency.USD, new BigDecimal("0.05"),
            Currency.EUR, new BigDecimal("0.05")));

    /** Fails on startup if a new provider category was added without a tariff. */
    public BillPaymentFeePolicy() {

        for (ProviderCategory category : ProviderCategory.values()) {
            if (!RATE_BY_CATEGORY.containsKey(category)) {
                throw new IllegalStateException("No bill payment rate for category " + category);
            }
        }
    }

    @Override
    public PaymentType paymentType() {

        return PaymentType.BILL_PAYMENT;
    }

    @Override
    public BigDecimal calculate(FeeRequest request) {

        BigDecimal percentageFee = request.amount()
                .multiply(RATE_BY_CATEGORY.get(request.providerCategory()))
                .setScale(2, RoundingMode.HALF_UP);

        return percentageFee.max(MINIMUM_FEE.get(request.currency()));
    }
}
