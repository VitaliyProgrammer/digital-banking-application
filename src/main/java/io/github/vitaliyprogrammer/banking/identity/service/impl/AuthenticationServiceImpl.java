package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.identity.dto.request.LoginRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RefreshRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RegisterRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.TokenResponseDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.identity.entity.User;
import io.github.vitaliyprogrammer.banking.identity.exception.EmailAlreadyTakenException;
import io.github.vitaliyprogrammer.banking.identity.exception.InvalidCredentialsException;
import io.github.vitaliyprogrammer.banking.identity.exception.InvalidRefreshTokenException;
import io.github.vitaliyprogrammer.banking.identity.exception.UserBlockedException;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.identity.service.AuthenticationService;
import io.github.vitaliyprogrammer.banking.identity.service.RefreshTokenService;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccessTokenService accessTokenService;

    private final RefreshTokenService refreshTokenService;

    private final Clock clock;

    /** Checked when the email is unknown, so that case takes as long as a wrong password. */
    private final String dummyPasswordHash;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AccessTokenService accessTokenService,
                                     RefreshTokenService refreshTokenService,
                                     Clock clock) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
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

    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {

        Optional<User> user = userRepository.findByEmail(normalizeEmail(request.email()));

        String passwordHash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        if (user.get().isBlocked()) {
            throw new UserBlockedException();
        }

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
