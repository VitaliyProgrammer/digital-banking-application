package io.github.vitaliyprogrammer.banking.card.dto.response;

/** Full card details for the owner only, e.g. to pay online. Returned on an explicit request, never in lists. */
public record CardDetailsResponseDto(
        String number,
        String expiry,
        String cvv
) {
}
