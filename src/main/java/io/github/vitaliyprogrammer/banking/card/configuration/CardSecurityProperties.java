package io.github.vitaliyprogrammer.banking.card.configuration;

import jakarta.validation.constraints.NotBlank;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Four independent 256-bit keys, one per purpose (key separation): a leak of the lookup key
 * does not let anyone decrypt card numbers, and a leak of the encryption key does not reveal CVVs.
 * The application refuses to start if any key is missing or not exactly 32 bytes.
 */
@Validated
@ConfigurationProperties(prefix = "banking.card")
public record CardSecurityProperties(
        @NotBlank
        String encryptionKey,

        @NotBlank
        String lookupKey,

        @NotBlank
        String cvvKey,

        @NotBlank
        String pinKey
) {

    private static final int KEY_LENGTH_BYTES = 32;

    public CardSecurityProperties {
        requireKeyLength("encryption-key", encryptionKey);
        requireKeyLength("lookup-key", lookupKey);
        requireKeyLength("cvv-key", cvvKey);
        requireKeyLength("pin-key", pinKey);
    }

    public static byte[] decode(String base64Key) {

        return Base64.getDecoder().decode(base64Key);
    }

    private static void requireKeyLength(String name, String base64Key) {

        if (base64Key != null && decode(base64Key).length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("banking.card." + name + " must be a Base64-encoded 32-byte key");
        }
    }
}
