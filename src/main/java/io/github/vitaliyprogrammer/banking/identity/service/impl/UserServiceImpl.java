package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.identity.exception.UserNotFoundException;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(UUID userId) {

        return userRepository.findById(userId)
                .map(UserResponseDto::from)
                .orElseThrow(UserNotFoundException::new);
    }
}
