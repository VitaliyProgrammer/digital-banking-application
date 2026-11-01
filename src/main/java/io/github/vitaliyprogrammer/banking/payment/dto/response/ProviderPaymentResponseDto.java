package io.github.vitaliyprogrammer.banking.payment.dto.response;

/** Provider's answer: "ACCEPTED" or "REJECTED", with a reason for a rejection. */
public record ProviderPaymentResponseDto(
        String status,
        String reason
) {
}
