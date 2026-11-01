package io.github.vitaliyprogrammer.banking.payment.service;

import io.github.vitaliyprogrammer.banking.payment.dto.request.ProviderPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.ProviderCallResult;
import java.util.UUID;

/**
 * The only place that talks to providers over the network. It never throws for network or HTTP problems:
 * every outcome, including timeouts, comes back as a {@link ProviderCallResult} the caller must handle.
 */
public interface ProviderClient {

    ProviderCallResult send(String providerCode, ProviderPaymentRequestDto request);

    ProviderCallResult checkStatus(String providerCode, UUID reference);
}
