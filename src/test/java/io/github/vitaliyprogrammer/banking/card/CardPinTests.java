package io.github.vitaliyprogrammer.banking.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.card.configuration.CardSecurityProperties;
import io.github.vitaliyprogrammer.banking.card.dto.request.ChangePinRequestDto;
import io.github.vitaliyprogrammer.banking.card.dto.request.VerifyPinRequestDto;
import io.github.vitaliyprogrammer.banking.card.entity.Card;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.CardPinLockedException;
import io.github.vitaliyprogrammer.banking.card.exception.CurrentPinRequiredException;
import io.github.vitaliyprogrammer.banking.card.exception.PinNotSetException;
import io.github.vitaliyprogrammer.banking.card.exception.WeakPinException;
import io.github.vitaliyprogrammer.banking.card.exception.WrongPinException;
import io.github.vitaliyprogrammer.banking.card.repository.CardRepository;
import io.github.vitaliyprogrammer.banking.card.service.impl.CardCryptography;
import io.github.vitaliyprogrammer.banking.card.service.impl.CardNumberGenerator;
import io.github.vitaliyprogrammer.banking.card.service.impl.CardServiceImpl;
import io.github.vitaliyprogrammer.banking.common.attempt.AttemptCounter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * PIN rules of {@link CardServiceImpl}: the counter (Redis in the application) and the database are mocked, the
 * cryptography is real. That a lock survives the exception (noRollbackFor) is proven in
 * {@code CardSecurityIntegrationTests}; here: how the attempts are counted and when the card locks.
 */
@ExtendWith(MockitoExtension.class)
class CardPinTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private static final String PIN = "4826";

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AttemptCounter attemptCounter;

    private final CardCryptography cryptography = new CardCryptography(new CardSecurityProperties(
            key(1), key(2), key(3), key(4)));

    private CardServiceImpl cardService;

    private final UUID ownerId = UUID.randomUUID();

    private Card card;

    private String failuresKey;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(cardRepository, accountService, new CardNumberGenerator(), cryptography,
                attemptCounter, Clock.fixed(NOW, ZoneOffset.UTC));

        card = Card.issue(ownerId, UUID.randomUUID(), CardType.UNIVERSAL, "encrypted", "lookup", "1234",
                LocalDate.of(2030, 11, 30), NOW);
        ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
        failuresKey = "card:pin-failures:" + card.getId();
    }

    @Test
    void rightPinClearsEarlierFailures() {
        withPin(PIN);

        cardService.verifyPin(ownerId, card.getId(), new VerifyPinRequestDto(PIN));

        verify(attemptCounter).reset(failuresKey);
        verify(attemptCounter, never()).increment(anyString(), any());
    }

    @Test
    void wrongPinSaysHowManyAttemptsAreLeft() {
        withPin(PIN);
        when(attemptCounter.increment(eq(failuresKey), any(Duration.class))).thenReturn(1L);

        assertThatThrownBy(() -> cardService.verifyPin(ownerId, card.getId(), new VerifyPinRequestDto("0007")))
                .isInstanceOf(WrongPinException.class)
                .hasMessageContaining("Attempts left: 2");
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void thirdWrongPinLocksTheCard() {
        withPin(PIN);
        when(attemptCounter.increment(eq(failuresKey), any(Duration.class))).thenReturn(3L);

        assertThatThrownBy(() -> cardService.verifyPin(ownerId, card.getId(), new VerifyPinRequestDto("0007")))
                .isInstanceOf(CardPinLockedException.class);

        assertThat(card.getStatus()).isEqualTo(CardStatus.PIN_LOCKED);
        // The lock in the database is what lasts; the counter starts again for the day the card is unlocked.
        verify(attemptCounter).reset(failuresKey);
    }

    @Test
    void lockedCardRefusesEvenTheRightPin() {
        withPin(PIN);
        card.lockAfterWrongPins();

        assertThatThrownBy(() -> cardService.verifyPin(ownerId, card.getId(), new VerifyPinRequestDto(PIN)))
                .isInstanceOf(CardPinLockedException.class);

        // Otherwise someone could keep guessing on a locked card and learn the PIN from which answer differs.
        verifyNoInteractions(attemptCounter);
    }

    @Test
    void cardWithoutPinCannotBeChecked() {
        found();

        assertThatThrownBy(() -> cardService.verifyPin(ownerId, card.getId(), new VerifyPinRequestDto(PIN)))
                .isInstanceOf(PinNotSetException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "1111", "1234", "6789", "9876", "3210"})
    void weakPinIsRefusedBeforeTheCardIsEvenRead(String weak) {
        assertThatThrownBy(() -> cardService.changePin(ownerId, card.getId(), new ChangePinRequestDto(null, weak)))
                .isInstanceOf(WeakPinException.class);

        verifyNoInteractions(cardRepository);
    }

    @Test
    void firstPinNeedsNoCurrentOne() {
        found();

        cardService.changePin(ownerId, card.getId(), new ChangePinRequestDto(null, PIN));

        assertThat(cryptography.pinMatches(card.getId(), PIN, card.getPinHash())).isTrue();
    }

    @Test
    void changingAPinNeedsTheCurrentOne() {
        withPin(PIN);

        assertThatThrownBy(() -> cardService.changePin(ownerId, card.getId(), new ChangePinRequestDto(null, "5937")))
                .isInstanceOf(CurrentPinRequiredException.class);
        assertThat(cryptography.pinMatches(card.getId(), PIN, card.getPinHash())).isTrue();
    }

    @Test
    void wrongCurrentPinCountsLikeAnyWrongPin() {
        withPin(PIN);
        when(attemptCounter.increment(eq(failuresKey), any(Duration.class))).thenReturn(1L);

        assertThatThrownBy(() -> cardService.changePin(ownerId, card.getId(), new ChangePinRequestDto("0007", "5937")))
                .isInstanceOf(WrongPinException.class);
        // Changing the PIN must not become a way around the attempt limit.
        assertThat(cryptography.pinMatches(card.getId(), PIN, card.getPinHash())).isTrue();
    }

    private void withPin(String pin) {
        card.changePin(cryptography.pinHash(card.getId(), pin));
        found();
    }

    private void found() {
        when(cardRepository.findByIdAndOwnerIdForUpdate(card.getId(), ownerId)).thenReturn(Optional.of(card));
    }

    private static String key(int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
