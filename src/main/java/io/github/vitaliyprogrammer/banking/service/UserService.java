package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;
import java.util.UUID;

public interface UserService {

    UserResponseDto getById(UUID userId);

    /** Creates an admin with this email unless an account with it already exists. */
    void createAdminIfAbsent(String email, String password);
}
