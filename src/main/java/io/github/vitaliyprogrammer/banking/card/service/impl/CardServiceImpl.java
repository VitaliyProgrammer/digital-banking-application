package io.github.vitaliyprogrammer.banking.card.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.card.dto.request.ChangePinRequestDto;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.dto.request.VerifyPinRequestDto;
import io.github.vitaliyprogrammer.banking.card.dto.response.CardDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.card.dto.response.CardReferenceDto;
import io.github.vitaliyprogrammer.banking.card.dto.response.CardResponseDto;
import io.github.vitaliyprogrammer.banking.card.entity.Card;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.CardAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.card.exception.CardNotFoundException;
import io.github.vitaliyprogrammer.banking.card.exception.CardPinLockedException;
import io.github.vitaliyprogrammer.banking.card.exception.CardTypeNotIssuableException;
import io.github.vitaliyprogrammer.banking.card.exception.CurrentPinRequiredException;
import io.github.vitaliyprogrammer.banking.card.exception.InvalidCardStateException;
import io.github.vitaliyprogrammer.banking.card.exception.PinNotSetException;
import io.github.vitaliyprogrammer.banking.card.exception.WeakPinException;
import io.github.vitaliyprogrammer.banking.card.exception.WrongPinException;
import io.github.vitaliyprogrammer.banking.card.repository.CardRepository;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.common.attempt.AttemptCounter;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private static final int VALIDITY_YEARS = 4;

    private static final int MAX_NUMBER_GENERATION_ATTEMPTS = 5;

    private static final String PIN_FAILURES_KEY_PREFIX = "card:pin-failures:";

    private static final int MAX_PIN_FAILURES = 3;

    private static final Duration PIN_FAILURES_WINDOW = Duration.ofHours(24);

    private final CardRepository cardRepository;

    private final AccountService accountService;

    private final CardNumberGenerator cardNumberGenerator;

    private final CardCryptography cardCryptography;

    private final AttemptCounter attemptCounter;

    private final Clock clock;

    @Override
    @Transactional
    public CardResponseDto issue(UUID ownerId, IssueCardRequestDto request) {

        if (!request.type().isIssuedOnRequest()) {
            throw new CardTypeNotIssuableException(request.type());
        }

        return issue(ownerId, accountService.getOwnedAccountReference(ownerId, request.accountId()), request.type());
    }

    @Override
    @Transactional
    public CardResponseDto issueCreditCard(UUID ownerId) {

        return issue(ownerId, accountService.getCreditAccountReference(ownerId), CardType.CREDIT);
    }

    private CardResponseDto issue(UUID ownerId, AccountReferenceDto account, CardType type) {

        if (cardRepository.existsByAccountIdAndTypeAndStatusNot(account.id(), type, CardStatus.CLOSED)) {
            throw new CardAlreadyExistsException(type);
        }

        String cardNumber = generateUniqueCardNumber();
        LocalDate expiresOn = YearMonth.now(clock).plusYears(VALIDITY_YEARS).atEndOfMonth();

        Card card = Card.issue(
                ownerId,
                account.id(),
                type,
                cardCryptography.encryptCardNumber(cardNumber),
                cardCryptography.cardNumberLookupHash(cardNumber),
                cardNumber.substring(cardNumber.length() - 4),
                expiresOn,
                clock.instant());

        try {
            return CardResponseDto.from(cardRepository.saveAndFlush(card));
        } catch (DataIntegrityViolationException e) {
            // Two parallel requests for the same card type: the partial unique index lets only one in.
            throw new CardAlreadyExistsException(type);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCards(UUID ownerId) {

        return cardRepository.findAllByOwnerIdOrderByCreatedAt(ownerId).stream()
                .map(CardResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponseDto getCard(UUID ownerId, UUID cardId) {

        return CardResponseDto.from(findOwnedCard(ownerId, cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public CardDetailsResponseDto getCardDetails(UUID ownerId, UUID cardId) {

        Card card = findOwnedCard(ownerId, cardId);

        if (card.getStatus() == CardStatus.CLOSED) {
            throw new InvalidCardStateException(card.getStatus());
        }

        String cardNumber = cardCryptography.decryptCardNumber(card.getPanEncrypted());

        return new CardDetailsResponseDto(
                cardNumber.replaceAll("(\\d{4})(?=\\d)", "$1 "),
                card.getExpiresOn().format(CardResponseDto.EXPIRY_FORMAT),
                cardCryptography.cvv(cardNumber, YearMonth.from(card.getExpiresOn())));
    }

    @Override
    @Transactional
    public CardResponseDto block(UUID ownerId, UUID cardId) {

        Card card = findOwnedCard(ownerId, cardId);
        card.block();

        return CardResponseDto.from(card);
    }

    @Override
    @Transactional
    public CardResponseDto unblock(UUID ownerId, UUID cardId) {

        Card card = findOwnedCard(ownerId, cardId);
        card.unblock();

        return CardResponseDto.from(card);
    }

    @Override
    @Transactional
    public CardResponseDto close(UUID ownerId, UUID cardId) {

        Card card = findOwnedCard(ownerId, cardId);
        card.close();

        return CardResponseDto.from(card);
    }

    /** Same noRollbackFor as verifyPin: a wrong current PIN must still count and may still lock the card. */
    @Override
    @Transactional(noRollbackFor = {WrongPinException.class, CardPinLockedException.class})
    public void changePin(UUID ownerId, UUID cardId, ChangePinRequestDto request) {

        if (isWeak(request.newPin())) {
            throw new WeakPinException();
        }

        Card card = findOwnedCardForUpdate(ownerId, cardId);

        if (card.isPinSet()) {
            if (request.currentPin() == null) {
                throw new CurrentPinRequiredException();
            }
            checkPin(card, request.currentPin());
        }

        card.changePin(cardCryptography.pinHash(card.getId(), request.newPin()));
    }

    /**
     * Simulates a terminal checking the PIN. The third wrong PIN locks the card, and that lock must be saved
     * even though the request ends with an exception: noRollbackFor keeps the transaction committing.
     */
    @Override
    @Transactional(noRollbackFor = {WrongPinException.class, CardPinLockedException.class})
    public void verifyPin(UUID ownerId, UUID cardId, VerifyPinRequestDto request) {

        checkPin(findOwnedCardForUpdate(ownerId, cardId), request.pin());
    }

    @Override
    @Transactional(readOnly = true)
    public CardReferenceDto getOwnedCardReference(UUID ownerId, UUID cardId) {

        return toReference(findOwnedCard(ownerId, cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public CardReferenceDto getCardReference(UUID cardId) {

        return cardRepository.findById(cardId)
                .map(this::toReference)
                .orElseThrow(CardNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CardReferenceDto> findCardByNumber(String cardNumber) {

        if (!CardNumberGenerator.isValid(cardNumber)) {
            return Optional.empty();
        }

        return cardRepository.findByPanLookupHash(cardCryptography.cardNumberLookupHash(cardNumber))
                .map(this::toReference);
    }

    /**
     * The caller holds a row lock on the card, so PIN checks of one card never overlap.
     * Redis keeps the failure count with a 24-hour window; the card status in PostgreSQL is the lasting result.
     */
    private void checkPin(Card card, String pin) {

        if (card.getStatus() == CardStatus.PIN_LOCKED) {
            throw new CardPinLockedException();
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardStateException(card.getStatus());
        }

        if (!card.isPinSet()) {
            throw new PinNotSetException();
        }

        String failuresKey = PIN_FAILURES_KEY_PREFIX + card.getId();

        if (cardCryptography.pinMatches(card.getId(), pin, card.getPinHash())) {
            attemptCounter.reset(failuresKey);
            return;
        }

        long failures = attemptCounter.increment(failuresKey, PIN_FAILURES_WINDOW);

        if (failures >= MAX_PIN_FAILURES) {
            card.lockAfterWrongPins();
            attemptCounter.reset(failuresKey);
            throw new CardPinLockedException();
        }

        throw new WrongPinException(MAX_PIN_FAILURES - failures);
    }

    /** 1111, 1234 and 9876 are among the first PINs anyone tries. */
    private static boolean isWeak(String pin) {

        boolean allSame = pin.chars().distinct().count() == 1;
        boolean ascending = true;
        boolean descending = true;

        for (int i = 1; i < pin.length(); i++) {
            int step = pin.charAt(i) - pin.charAt(i - 1);
            ascending &= step == 1;
            descending &= step == -1;
        }

        return allSame || ascending || descending;
    }

    /** 9 random digits give a billion numbers per BIN, so a collision is rare; the unique index is the guarantee. */
    private String generateUniqueCardNumber() {

        for (int attempt = 0; attempt < MAX_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            String cardNumber = cardNumberGenerator.generate();
            if (!cardRepository.existsByPanLookupHash(cardCryptography.cardNumberLookupHash(cardNumber))) {
                return cardNumber;
            }
        }

        throw new IllegalStateException("Could not generate a unique card number");
    }

    private CardReferenceDto toReference(Card card) {

        return new CardReferenceDto(
                card.getId(),
                card.getOwnerId(),
                card.getAccountId(),
                card.getType(),
                card.getPanLastFour(),
                card.isUsable(LocalDate.now(clock)));
    }

    /** Ownership is part of the query: someone else's card is simply "not found". */
    private Card findOwnedCard(UUID ownerId, UUID cardId) {

        return cardRepository.findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(CardNotFoundException::new);
    }

    private Card findOwnedCardForUpdate(UUID ownerId, UUID cardId) {

        return cardRepository.findByIdAndOwnerIdForUpdate(cardId, ownerId)
                .orElseThrow(CardNotFoundException::new);
    }
}
