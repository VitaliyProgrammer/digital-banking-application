package io.github.vitaliyprogrammer.banking.dto.response;

/** Full card details for the owner only, e.g. to pay online. Returned on an explicit request, never in lists. */
public record CardDetailsResponseDto(
        String number,
        String expiry,
        String cvv
) {
}
