package io.github.vitaliyprogrammer.banking.account.service.impl;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Ukrainian IBAN (29 characters): UA + 2 check digits + 6-digit bank code + 19-digit account number.
 * Check digits follow ISO 13616 (mod 97), so the number passes the same validation as a real IBAN.
 */
@Component
public class IbanGenerator {

    /** Fictional bank code for this project. */
    private static final String BANK_CODE = "300999";

    private static final int ACCOUNT_NUMBER_LENGTH = 19;

    /** "UA00" with letters converted to numbers: U = 30, A = 10, then "00" in place of the check digits. */
    private static final String COUNTRY_CODE_AS_DIGITS = "301000";

    private static final BigInteger MODULUS = BigInteger.valueOf(97);

    public String generate() {

        String bban = BANK_CODE + randomDigits(ACCOUNT_NUMBER_LENGTH);

        // ISO 13616: move the country code to the end, then check digits = 98 - (number mod 97).
        int checkDigits = 98 - new BigInteger(bban + COUNTRY_CODE_AS_DIGITS).mod(MODULUS).intValue();

        return "UA" + String.format("%02d", checkDigits) + bban;
    }

    private static String randomDigits(int length) {

        StringBuilder digits = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            digits.append(ThreadLocalRandom.current().nextInt(10));
        }
        return digits.toString();
    }
}
