package io.github.vitaliyprogrammer.banking.payment.service.impl.fee;

import io.github.vitaliyprogrammer.banking.payment.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.service.FeePolicy;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Picks the {@link FeePolicy} for a payment type. Spring injects every FeePolicy bean into the constructor;
 * the application refuses to start if a payment type has no policy or has two of them.
 */
@Component
public class FeeCalculator {

    private final Map<PaymentType, FeePolicy> policies = new EnumMap<>(PaymentType.class);

    public FeeCalculator(List<FeePolicy> feePolicies) {

        for (FeePolicy policy : feePolicies) {
            FeePolicy duplicate = policies.put(policy.paymentType(), policy);
            if (duplicate != null) {
                throw new IllegalStateException("Two fee policies for " + policy.paymentType() + ": "
                        + duplicate.getClass().getSimpleName() + " and " + policy.getClass().getSimpleName());
            }
        }

        for (PaymentType type : PaymentType.values()) {
            if (!policies.containsKey(type)) {
                throw new IllegalStateException("No fee policy for payment type " + type);
            }
        }
    }

    public BigDecimal calculate(PaymentType type, FeeRequest request) {

        return policies.get(type).calculate(request);
    }
}
