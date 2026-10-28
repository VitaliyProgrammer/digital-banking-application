package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.CardSecurityProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Everything secret about a card, in one place. A real bank does this inside an HSM (hardware security module);
 * here the keys come from configuration, but the ideas are the same:
 * the number is encrypted, lookups use a keyed hash, the CVV is derived instead of stored, the PIN is a keyed hash.
 */
@Component
public class CardCryptography {

    private static final String CIPHER = "AES/GCM/NoPadding";

    private static final String HMAC = "HmacSHA256";

    private static final int IV_LENGTH_BYTES = 12;

    private static final int AUTH_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey encryptionKey;

    private final SecretKey lookupKey;

    private final SecretKey cvvKey;

    private final SecretKey pinKey;

    public CardCryptography(CardSecurityProperties properties) {

        this.encryptionKey = new SecretKeySpec(CardSecurityProperties.decode(properties.encryptionKey()), "AES");
        this.lookupKey = new SecretKeySpec(CardSecurityProperties.decode(properties.lookupKey()), HMAC);
        this.cvvKey = new SecretKeySpec(CardSecurityProperties.decode(properties.cvvKey()), HMAC);
        this.pinKey = new SecretKeySpec(CardSecurityProperties.decode(properties.pinKey()), HMAC);
    }

    /**
     * AES-256-GCM with a fresh random IV every time: the same number encrypts differently on each call,
     * and the auth tag makes any tampering with the stored value fail on decryption.
     */
    public String encryptCardNumber(String cardNumber) {

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Card number encryption failed", e);
        }
    }

    public String decryptCardNumber(String stored) {

        try {
            byte[] bytes = Base64.getDecoder().decode(stored);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, bytes, 0, IV_LENGTH_BYTES));

            return new String(cipher.doFinal(bytes, IV_LENGTH_BYTES, bytes.length - IV_LENGTH_BYTES),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Card number decryption failed", e);
        }
    }

    /**
     * Encryption with a random IV cannot be searched, so lookups by card number use a keyed hash instead.
     * Plain SHA-256 would not do: card numbers share a known prefix and a check digit, so all 10^9 possible
     * numbers of one BIN can be hashed in minutes. Without the secret key, an HMAC cannot be precomputed.
     */
    public String cardNumberLookupHash(String cardNumber) {

        return hex(hmac(lookupKey, cardNumber));
    }

    /**
     * The CVV is derived from the number and expiry month with a secret key, like issuers do in an HSM.
     * Nothing is stored, yet the same card always gets the same CVV, so it can be shown again and verified.
     */
    public String cvv(String cardNumber, YearMonth expiry) {

        byte[] mac = hmac(cvvKey, cardNumber + "|" + expiry);
        int value = Integer.remainderUnsigned(ByteBuffer.wrap(mac).getInt(), 1000);

        return String.format("%03d", value);
    }

    /**
     * A 4-digit PIN has only 10,000 values, so a slow hash like BCrypt would not stop anyone who has the table:
     * 10,000 guesses take minutes. What protects it is the secret key (not in the database)
     * and the attempt limit. The card id is mixed in, so equal PINs on different cards get different hashes.
     */
    public String pinHash(UUID cardId, String pin) {

        return hex(hmac(pinKey, cardId + "|" + pin));
    }

    /** Constant-time comparison: the time taken does not reveal how many leading characters matched. */
    public boolean pinMatches(UUID cardId, String pin, String storedHash) {

        return MessageDigest.isEqual(
                pinHash(cardId, pin).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /** A new Mac per call: Mac instances are not thread-safe. */
    private static byte[] hmac(SecretKey key, String value) {

        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(key);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC calculation failed", e);
        }
    }

    private static String hex(byte[] bytes) {

        return HexFormat.of().formatHex(bytes);
    }
}
