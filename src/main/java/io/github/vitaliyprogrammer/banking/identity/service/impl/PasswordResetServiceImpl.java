package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.common.attempt.AttemptCounter;
import io.github.vitaliyprogrammer.banking.identity.configuration.PasswordResetProperties;
import io.github.vitaliyprogrammer.banking.identity.dto.request.PasswordResetConfirmRequestDto;
import io.github.vitaliyprogrammer.banking.identity.entity.PasswordResetToken;
import io.github.vitaliyprogrammer.banking.identity.entity.User;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordChangedEvent;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordResetRequestedEvent;
import io.github.vitaliyprogrammer.banking.identity.exception.InvalidPasswordResetTokenException;
import io.github.vitaliyprogrammer.banking.identity.repository.PasswordResetTokenRepository;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.PasswordResetService;
import io.github.vitaliyprogrammer.banking.identity.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Forgot password", the way a bank must do it:
 * <ul>
 *   <li>the same answer for every address, so the form never reveals who has an account;</li>
 *   <li>256 random bits per link, only their SHA-256 in the database, 30 minutes, one use;</li>
 *   <li>a new request makes older links useless; a used link ends every session of the user;</li>
 *   <li>the email leaves after commit and on its own thread (see {@link PasswordResetMailer}).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String REQUESTS_KEY_PREFIX = "password-reset:requests:";

    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder;

    private final AttemptCounter attemptCounter;

    private final ApplicationEventPublisher eventPublisher;

    private final PasswordResetProperties properties;

    private final Clock clock;

    /**
     * Every "no" is silent: an unknown address, a blocked client, too many requests. Saying which would turn
     * this form into a way to check who banks here.
     */
    @Override
    @Transactional
    public void requestReset(String email) {

        String normalized = email.strip().toLowerCase(Locale.ROOT);

        if (attemptCounter.increment(REQUESTS_KEY_PREFIX + normalized, properties.requestWindow())
                > properties.maxRequests()) {
            log.warn("Password reset requests for one address over the limit; ignored");
            return;
        }

        User user = userRepository.findByEmail(normalized).filter(candidate -> !candidate.isBlocked()).orElse(null);
        if (user == null) {
            return;
        }

        Instant now = clock.instant();
        passwordResetTokenRepository.invalidateUnused(user.getId(), now);

        String rawToken = generateRawToken();
        passwordResetTokenRepository.save(
                PasswordResetToken.issue(user.getId(), hash(rawToken), now.plus(properties.tokenTtl()), now));

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getFirstName(), rawToken));
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequestDto request) {

        Instant now = clock.instant();
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashForUpdate(hash(request.token()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidPasswordResetTokenException::new);

        token.use(now);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // Whoever might have been in the account with the old password is out now.
        refreshTokenService.revokeAllSessions(user.getId());

        log.info("Password of {} reset through an emailed link", user.getId());
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getEmail(), user.getFirstName()));
    }

    private static String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Plain SHA-256 is enough: 256 random bits leave nothing to brute-force, and it allows an indexed lookup. */
    private static String hash(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
