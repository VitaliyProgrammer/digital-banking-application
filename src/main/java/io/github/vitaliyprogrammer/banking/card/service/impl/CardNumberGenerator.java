package io.github.vitaliyprogrammer.banking.card.service.impl;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 16-digit card numbers: 6-digit BIN (issuer prefix) + 9 random digits + 1 Luhn check digit,
 * the same structure that Visa and Mastercard use.
 */
@Component
public class CardNumberGenerator {

    /** Fictional BIN for this project; "4" is the Visa prefix. */
    private static final String BIN = "499999";

    private static final int CARD_NUMBER_LENGTH = 16;

    /** SecureRandom: card numbers must not be predictable from previously issued ones. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate() {

        StringBuilder digits = new StringBuilder(BIN);

        while (digits.length() < CARD_NUMBER_LENGTH - 1) {
            digits.append(SECURE_RANDOM.nextInt(10));
        }

        digits.append(luhnCheckDigit(digits));

        return digits.toString();
    }

    /** True for a 16-digit number whose last digit is the correct Luhn check digit. */
    public static boolean isValid(String cardNumber) {

        if (cardNumber == null || !cardNumber.matches("\\d{" + CARD_NUMBER_LENGTH + "}")) {
            return false;
        }

        int expected = luhnCheckDigit(cardNumber.substring(0, CARD_NUMBER_LENGTH - 1));

        return cardNumber.charAt(CARD_NUMBER_LENGTH - 1) - '0' == expected;
    }

    /**
     * Luhn (mod 10): starting from the rightmost digit of the payload, double every second digit
     * (subtracting 9 when the result is two-digit), sum everything, and pick the digit that makes the sum
     * a multiple of 10. It catches every single-digit typo and almost every swap of two neighbouring digits.
     */
    static int luhnCheckDigit(CharSequence payload) {

        int sum = 0;

        for (int i = payload.length() - 1, position = 0; i >= 0; i--, position++) {
            int digit = payload.charAt(i) - '0';
            if (position % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }

        return (10 - sum % 10) % 10;
    }
}
