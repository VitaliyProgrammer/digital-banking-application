package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.ChangePinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.VerifyPinRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardResponseDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardService {

    CardResponseDto issue(UUID ownerId, IssueCardRequestDto request);

    /** For the credit module only: a CREDIT card on the owner's credit line, issued when the limit is approved. */
    CardResponseDto issueCreditCard(UUID ownerId);

    List<CardResponseDto> getCards(UUID ownerId);

    CardResponseDto getCard(UUID ownerId, UUID cardId);

    CardDetailsResponseDto getCardDetails(UUID ownerId, UUID cardId);

    CardResponseDto block(UUID ownerId, UUID cardId);

    CardResponseDto unblock(UUID ownerId, UUID cardId);

    CardResponseDto close(UUID ownerId, UUID cardId);

    void changePin(UUID ownerId, UUID cardId, ChangePinRequestDto request);

    void verifyPin(UUID ownerId, UUID cardId, VerifyPinRequestDto request);

    /** For other modules: a card of the given owner, or 404. */
    CardReferenceDto getOwnedCardReference(UUID ownerId, UUID cardId);

    /** For other modules: any card by id, e.g. the recipient's card on a receipt. */
    CardReferenceDto getCardReference(UUID cardId);

    /** For other modules: any card by its 16-digit number, e.g. the recipient of a card-to-card transfer. */
    Optional<CardReferenceDto> findCardByNumber(String cardNumber);
}
