package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;
import java.util.UUID;

public interface UserService {

    UserResponseDto getById(UUID userId);
}
