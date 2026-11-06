package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.account.service.impl.IbanGenerator;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** Every generated number must pass the same check (ISO 13616, mod 97) that any bank runs on an incoming IBAN. */
class IbanGeneratorTests {

    private final IbanGenerator generator = new IbanGenerator();

    @Test
    void generatedIbansAreValidUkrainianIbansOfThisBank() {
        for (int i = 0; i < 1_000; i++) {
            String iban = generator.generate();

            assertThat(iban).hasSize(29).startsWith("UA").matches("UA\\d{27}");
            assertThat(iban.substring(4, 10)).as("bank code").isEqualTo("300999");
            assertThat(isValid(iban)).as(iban).isTrue();
        }
    }

    @Test
    void checkCatchesAMistypedDigit() {
        // The well-known sample Ukrainian IBAN proves the check itself, not just our generator.
        assertThat(isValid("UA213223130000026007233566001")).isTrue();
        assertThat(isValid("UA213223130000026007233566002")).isFalse();
    }

    /** ISO 13616: move the first four characters to the end, letters become numbers (A = 10 ... Z = 35), mod 97 is 1. */
    private static boolean isValid(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder digits = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
