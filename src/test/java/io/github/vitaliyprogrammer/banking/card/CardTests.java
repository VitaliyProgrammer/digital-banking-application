package io.github.vitaliyprogrammer.banking.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.card.entity.Card;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.InvalidCardStateException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A card's life: which status changes are allowed, and until which day it pays. */
class CardTests {

    private static final LocalDate EXPIRES_ON = LocalDate.of(2030, 11, 30);

    @Test
    void newCardIsActiveWithoutPin() {
        Card card = newCard();

        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.isPinSet()).isFalse();
    }

    @Test
    void blockedCardCanBeUnblocked() {
        Card card = newCard();

        card.block();
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);

        card.unblock();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void onlyAnActiveCardIsBlockedAndOnlyABlockedOneUnblocked() {
        Card card = newCard();

        assertThatThrownBy(card::unblock).isInstanceOf(InvalidCardStateException.class);

        card.block();
        assertThatThrownBy(card::block).isInstanceOf(InvalidCardStateException.class);
    }

    @Test
    void closedCardNeverComesBack() {
        Card card = newCard();
        card.close();

        assertThatThrownBy(card::unblock).isInstanceOf(InvalidCardStateException.class);
        assertThatThrownBy(card::block).isInstanceOf(InvalidCardStateException.class);
        assertThatThrownBy(card::close).isInstanceOf(InvalidCardStateException.class);
        assertThat(card.getStatus()).isEqualTo(CardStatus.CLOSED);
    }

    @Test
    void pinChangesOnlyOnAnActiveCard() {
        Card card = newCard();
        card.changePin("hash-1");
        assertThat(card.isPinSet()).isTrue();

        card.block();
        assertThatThrownBy(() -> card.changePin("hash-2")).isInstanceOf(InvalidCardStateException.class);
        assertThat(card.getPinHash()).isEqualTo("hash-1");
    }

    @Test
    void wrongPinsLockOnlyAnActiveCard() {
        Card active = newCard();
        active.lockAfterWrongPins();
        assertThat(active.getStatus()).isEqualTo(CardStatus.PIN_LOCKED);

        // A blocked card stays blocked: the owner's own block must not turn into a PIN lock.
        Card blocked = newCard();
        blocked.block();
        blocked.lockAfterWrongPins();
        assertThat(blocked.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void cardPaysUntilItsLastDayAndOnlyWhileActive() {
        Card card = newCard();

        assertThat(card.isUsable(EXPIRES_ON)).isTrue();
        assertThat(card.isUsable(EXPIRES_ON.plusDays(1))).isFalse();

        card.block();
        assertThat(card.isUsable(EXPIRES_ON.minusYears(1))).isFalse();
    }

    private static Card newCard() {
        return Card.issue(UUID.randomUUID(), UUID.randomUUID(), CardType.UNIVERSAL,
                "encrypted", "lookup-hash", "1234", EXPIRES_ON, Instant.parse("2026-11-06T10:00:00Z"));
    }
}
