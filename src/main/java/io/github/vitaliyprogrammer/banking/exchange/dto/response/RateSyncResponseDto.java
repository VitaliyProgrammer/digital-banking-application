package io.github.vitaliyprogrammer.banking.exchange.dto.response;

/** How many new official rates the sync stored; 0 means today's rates were already there. */
public record RateSyncResponseDto(int savedRates) {
}
