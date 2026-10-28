package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.JwtProperties;
import io.github.vitaliyprogrammer.banking.entity.RefreshToken;
import io.github.vitaliyprogrammer.banking.exception.InvalidRefreshTokenException;
import io.github.vitaliyprogrammer.banking.repository.RefreshTokenRepository;
import io.github.vitaliyprogrammer.banking.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtProperties properties;

    private final Clock clock;

    /** Creates a session and returns the raw token. The raw value is shown to the client once and never stored. */
    @Override
    @Transactional
    public String issue(UUID userId) {

        String rawToken = generateRawToken();

        Instant now = clock.instant();

        refreshTokenRepository.save(
                RefreshToken.issue(userId, hash(rawToken), now.plus(properties.refreshTokenTtl()), now));

        return rawToken;
    }

    /**
     * Rotation: the presented token is revoked and the owner's id is returned, so the caller issues a new pair.
     * A token that is already revoked means it was stolen or replayed, so all sessions of that user are revoked.
     * {@code noRollbackFor} keeps that mass revocation committed even though the method ends with an exception.
     */
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public UUID consume(String rawToken) {

        Instant now = clock.instant();

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllActive(token.getUserId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke(now);

        return token.getUserId();
    }

    /** Logout. Unknown or already revoked tokens are ignored: logging out twice is not an error. */
    @Override
    @Transactional
    public void revoke(String rawToken) {

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Plain SHA-256 is enough here, unlike passwords: the token is 256 random bits, so there is nothing
     * to brute-force, and a fast hash lets us find the token by an indexed equality lookup.
     */
    private static String hash(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
