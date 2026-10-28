package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.LoginRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.RefreshRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.RegisterRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.TokenResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.entity.User;
import io.github.vitaliyprogrammer.banking.exception.EmailAlreadyTakenException;
import io.github.vitaliyprogrammer.banking.exception.InvalidCredentialsException;
import io.github.vitaliyprogrammer.banking.exception.InvalidRefreshTokenException;
import io.github.vitaliyprogrammer.banking.exception.TooManyLoginAttemptsException;
import io.github.vitaliyprogrammer.banking.exception.UserBlockedException;
import io.github.vitaliyprogrammer.banking.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.service.AttemptCounter;
import io.github.vitaliyprogrammer.banking.service.AuthenticationService;
import io.github.vitaliyprogrammer.banking.service.RefreshTokenService;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String LOGIN_ATTEMPTS_KEY_PREFIX = "login:attempts:";

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private static final Duration LOGIN_ATTEMPTS_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccessTokenService accessTokenService;

    private final RefreshTokenService refreshTokenService;

    private final AttemptCounter attemptCounter;

    private final Clock clock;

    /** Checked when the email is unknown, so that case takes as long as a wrong password. */
    private final String dummyPasswordHash;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AccessTokenService accessTokenService,
                                     RefreshTokenService refreshTokenService,
                                     AttemptCounter attemptCounter,
                                     Clock clock) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.attemptCounter = attemptCounter;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyTakenException();
        }

        User user = User.newClient(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                clock.instant());

        try {
            return UserResponseDto.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two parallel registrations with the same email both passed the check above;
            // the unique constraint lets only one of them in.
            throw new EmailAlreadyTakenException();
        }
    }

    /**
     * Every attempt is counted before the password is checked, and a successful login resets the counter.
     * Counting first matters: 100 parallel guesses get attempt numbers 1..100 atomically, so only the first 5
     * ever reach the (expensive) password check. Checking "failures < 5" first would let all 100 through.
     */
    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {

        String email = normalizeEmail(request.email());
        String attemptsKey = LOGIN_ATTEMPTS_KEY_PREFIX + email;

        if (attemptCounter.increment(attemptsKey, LOGIN_ATTEMPTS_WINDOW) > MAX_LOGIN_ATTEMPTS) {
            throw new TooManyLoginAttemptsException();
        }

        Optional<User> user = userRepository.findByEmail(email);

        String passwordHash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        if (user.get().isBlocked()) {
            throw new UserBlockedException();
        }

        attemptCounter.reset(attemptsKey);

        return issueTokens(user.get());
    }

    /**
     * Same noRollbackFor as {@link RefreshTokenService#consume}: otherwise this outer transaction would roll back
     * the "revoke all sessions" reaction to a reused token.
     */
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponseDto refresh(RefreshRequestDto request) {

        UUID userId = refreshTokenService.consume(request.refreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (user.isBlocked()) {
            throw new UserBlockedException();
        }

        return issueTokens(user);
    }

    @Override
    public void logout(RefreshRequestDto request) {

        refreshTokenService.revoke(request.refreshToken());
    }

    private TokenResponseDto issueTokens(User user) {

        return TokenResponseDto.bearer(
                accessTokenService.issue(user),
                refreshTokenService.issue(user.getId()),
                accessTokenService.ttlSeconds());
    }

    /** Locale.ROOT: with a Turkish default locale "I".toLowerCase() is "ı", not "i". */
    private static String normalizeEmail(String email) {

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
