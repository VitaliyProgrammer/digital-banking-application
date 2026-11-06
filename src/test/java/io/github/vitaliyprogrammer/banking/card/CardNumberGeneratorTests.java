package io.github.vitaliyprogrammer.banking.card;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.card.service.impl.CardNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Card numbers carry a Luhn check digit, like every Visa and Mastercard: a recipient's number with a typo is
 * refused before the bank even looks it up.
 */
class CardNumberGeneratorTests {

    private final CardNumberGenerator generator = new CardNumberGenerator();

    @Test
    void generatedNumbersAreValidAndCarryTheBankBin() {
        for (int i = 0; i < 1_000; i++) {
            String number = generator.generate();

            assertThat(number).hasSize(16).startsWith("499999");
            assertThat(CardNumberGenerator.isValid(number)).as(number).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111111", "5555555555554444", "4012888888881881"})
    void wellKnownTestCardNumbersAreValid(String number) {
        // Public test numbers of the card networks: they prove the algorithm, not just our own output.
        assertThat(CardNumberGenerator.isValid(number)).isTrue();
    }

    @Test
    void anyMistypedDigitIsCaught() {
        String number = generator.generate();

        for (int position = 0; position < number.length(); position++) {
            char wrong = (char) ('0' + (number.charAt(position) - '0' + 1) % 10);
            String typo = number.substring(0, position) + wrong + number.substring(position + 1);

            assertThat(CardNumberGenerator.isValid(typo)).as("typo at position %d", position).isFalse();
        }
    }

    @Test
    void swappedNeighbouringDigitsAreCaught() {
        String number = "4111111111111111";
        String swapped = "1411111111111111";

        assertThat(CardNumberGenerator.isValid(swapped)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "411111111111111", "41111111111111111", "4111 1111 1111 1111", "411111111111111a"})
    void anythingButSixteenDigitsIsInvalid(String number) {
        assertThat(CardNumberGenerator.isValid(number)).isFalse();
    }
}
