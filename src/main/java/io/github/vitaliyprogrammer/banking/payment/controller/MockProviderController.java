package io.github.vitaliyprogrammer.banking.payment.controller;

import io.github.vitaliyprogrammer.banking.payment.dto.request.ProviderPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.ProviderPaymentResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FAKE external providers for local runs and demos, NOT part of the bank's API. The bank calls them over real
 * HTTP, exactly as it would call a real provider. The ending of the personal account number picks a scenario:
 * "...000" → rejected, "...999" → accepted but answers too late (the bank sees a timeout), anything else → accepted.
 * Disabled with banking.providers.mock-enabled=false.
 */
@Hidden
@RestController
@RequestMapping("/mock-providers")
@ConditionalOnProperty(name = "banking.providers.mock-enabled", havingValue = "true")
public class MockProviderController {

    private static final Duration SLOW_ANSWER = Duration.ofSeconds(5);

    /** Payments "received" by the fake providers, by reference. In memory: a restart forgets them. */
    private final Map<UUID, ProviderPaymentResponseDto> received = new ConcurrentHashMap<>();

    /** Like a real provider, a repeated reference returns the first answer instead of paying twice. */
    @PostMapping("/{providerCode}/payments")
    public ProviderPaymentResponseDto receivePayment(@PathVariable String providerCode,
                                                     @RequestBody ProviderPaymentRequestDto request)
            throws InterruptedException {

        ProviderPaymentResponseDto answer = received.computeIfAbsent(request.reference(),
                reference -> request.accountNumber().endsWith("000")
                        ? new ProviderPaymentResponseDto("REJECTED", "Unknown personal account")
                        : new ProviderPaymentResponseDto("ACCEPTED", null));

        if (request.accountNumber().endsWith("999")) {
            Thread.sleep(SLOW_ANSWER);
        }

        return answer;
    }

    @GetMapping("/{providerCode}/payments/{reference}")
    public ResponseEntity<ProviderPaymentResponseDto> getPayment(@PathVariable String providerCode,
                                                                 @PathVariable UUID reference) {

        ProviderPaymentResponseDto answer = received.get(reference);

        return answer != null ? ResponseEntity.ok(answer) : ResponseEntity.notFound().build();
    }
}
