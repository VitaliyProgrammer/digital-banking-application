package io.github.vitaliyprogrammer.banking.dto.response;

/** Provider's answer: "ACCEPTED" or "REJECTED", with a reason for a rejection. */
public record ProviderPaymentResponseDto(
        String status,
        String reason
) {
}
