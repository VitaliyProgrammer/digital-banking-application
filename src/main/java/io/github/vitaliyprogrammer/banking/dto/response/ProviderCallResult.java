package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.status.ProviderOutcome;

/** A provider call translated into what it means for us, whatever HTTP, timeout or network error caused it. */
public record ProviderCallResult(
        ProviderOutcome outcome,
        String detail
) {

    public static ProviderCallResult of(ProviderOutcome outcome) {
        return new ProviderCallResult(outcome, null);
    }
}
