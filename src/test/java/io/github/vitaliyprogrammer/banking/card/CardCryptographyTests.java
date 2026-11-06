package io.github.vitaliyprogrammer.banking.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.card.configuration.CardSecurityProperties;
import io.github.vitaliyprogrammer.banking.card.service.impl.CardCryptography;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The card secrets, checked one property at a time: what must stay equal, what must differ, what must fail. */
class CardCryptographyTests {

    private static final String NUMBER = "4999991234567893";

    private final CardCryptography cryptography = new CardCryptography(new CardSecurityProperties(
            key(1), key(2), key(3), key(4)));

    @Test
    void numberIsEncryptedDifferentlyEachTimeAndDecryptsBack() {
        String first = cryptography.encryptCardNumber(NUMBER);
        String second = cryptography.encryptCardNumber(NUMBER);

        // A fresh IV every time: equal ciphertexts would show which cards share a number.
        assertThat(first).isNotEqualTo(second).doesNotContain(NUMBER);
        assertThat(cryptography.decryptCardNumber(first)).isEqualTo(NUMBER);
        assertThat(cryptography.decryptCardNumber(second)).isEqualTo(NUMBER);
    }

    @Test
    void tamperedCiphertextIsRefused() {
        byte[] stored = Base64.getDecoder().decode(cryptography.encryptCardNumber(NUMBER));
        stored[stored.length - 1] ^= 1;

        // GCM's auth tag: a changed byte is never decrypted into a different, plausible card number.
        assertThatThrownBy(() -> cryptography.decryptCardNumber(Base64.getEncoder().encodeToString(stored)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lookupHashIsStableSoACardCanBeFoundByItsNumber() {
        assertThat(cryptography.cardNumberLookupHash(NUMBER)).isEqualTo(cryptography.cardNumberLookupHash(NUMBER));
        assertThat(cryptography.cardNumberLookupHash(NUMBER))
                .isNotEqualTo(cryptography.cardNumberLookupHash("4999991234567885"));
    }

    @Test
    void cvvIsThreeDigitsAndTheSameEveryTimeForOneCard() {
        YearMonth expiry = YearMonth.of(2030, 11);

        String cvv = cryptography.cvv(NUMBER, expiry);

        assertThat(cvv).matches("\\d{3}").isEqualTo(cryptography.cvv(NUMBER, expiry));
        // A reissued card (new expiry) gets a new CVV.
        assertThat(cryptography.cvv(NUMBER, expiry.plusYears(4))).isNotEqualTo(cvv);
    }

    @Test
    void samePinOnTwoCardsGivesDifferentHashes() {
        UUID firstCard = UUID.randomUUID();
        UUID secondCard = UUID.randomUUID();

        String hash = cryptography.pinHash(firstCard, "4826");

        assertThat(hash).isNotEqualTo(cryptography.pinHash(secondCard, "4826"));
        assertThat(cryptography.pinMatches(firstCard, "4826", hash)).isTrue();
        assertThat(cryptography.pinMatches(firstCard, "4827", hash)).isFalse();
        assertThat(cryptography.pinMatches(secondCard, "4826", hash)).isFalse();
    }

    @Test
    void keyOfTheWrongLengthStopsTheApplication() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new CardSecurityProperties(shortKey, key(2), key(3), key(4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryption-key");
    }

    /** A 256-bit test key; a different fill per purpose, so the keys are not accidentally the same. */
    private static String key(int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
