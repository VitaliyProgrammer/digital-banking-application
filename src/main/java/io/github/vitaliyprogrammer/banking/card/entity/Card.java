package io.github.vitaliyprogrammer.banking.card.entity;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.InvalidCardStateException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * A payment card linked to an account. Holds no secret in plain text: the number is encrypted,
 * the PIN is a keyed hash, and the CVV is not stored at all.
 */
@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID accountId;

    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    private CardType type;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    private String panEncrypted;

    private String panLookupHash;

    private String panLastFour;

    private LocalDate expiresOn;

    private String pinHash;

    private Instant createdAt;

    public static Card issue(UUID ownerId,
                             UUID accountId,
                             CardType type,
                             String panEncrypted,
                             String panLookupHash,
                             String panLastFour,
                             LocalDate expiresOn,
                             Instant now) {
        Card card = new Card();
        card.ownerId = ownerId;
        card.accountId = accountId;
        card.type = type;
        card.status = CardStatus.ACTIVE;
        card.panEncrypted = panEncrypted;
        card.panLookupHash = panLookupHash;
        card.panLastFour = panLastFour;
        card.expiresOn = expiresOn;
        card.createdAt = now;
        return card;
    }

    public boolean isPinSet() {

        return pinHash != null;
    }

    /** Usable for payments: active and not past the last day of its expiry month. */
    public boolean isUsable(LocalDate today) {

        return status == CardStatus.ACTIVE && !today.isAfter(expiresOn);
    }

    public void changePin(String newPinHash) {

        requireStatus(CardStatus.ACTIVE);
        this.pinHash = newPinHash;
    }

    public void block() {

        requireStatus(CardStatus.ACTIVE);
        this.status = CardStatus.BLOCKED;
    }

    public void unblock() {

        requireStatus(CardStatus.BLOCKED);
        this.status = CardStatus.ACTIVE;
    }

    public void close() {

        if (status == CardStatus.CLOSED) {
            throw new InvalidCardStateException(status);
        }

        this.status = CardStatus.CLOSED;
    }

    public void lockAfterWrongPins() {

        if (status == CardStatus.ACTIVE) {
            this.status = CardStatus.PIN_LOCKED;
        }
    }

    private void requireStatus(CardStatus expected) {

        if (status != expected) {
            throw new InvalidCardStateException(status);
        }
    }
}
