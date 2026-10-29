package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.entity.User;
import io.github.vitaliyprogrammer.banking.entity.status.Role;
import io.github.vitaliyprogrammer.banking.exception.UserNotFoundException;
import io.github.vitaliyprogrammer.banking.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.service.UserService;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(UUID userId) {

        return userRepository.findById(userId)
                .map(UserResponseDto::from)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional
    public void createAdminIfAbsent(String email, String password) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);

        if (existing.isEmpty()) {
            userRepository.save(User.newAdmin(normalizedEmail, passwordEncoder.encode(password), clock.instant()));
            log.info("Admin account {} created", normalizedEmail);
            return;
        }

        // Promoting an existing account would hand admin rights to whoever registered this email first.
        if (existing.get().getRole() != Role.ADMIN) {
            log.warn("Account {} exists with role {} and is NOT promoted to admin; use another ADMIN_EMAIL",
                    normalizedEmail, existing.get().getRole());
        }
    }
}
