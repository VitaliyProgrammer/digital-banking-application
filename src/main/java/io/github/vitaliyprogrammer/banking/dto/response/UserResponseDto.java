package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.User;
import io.github.vitaliyprogrammer.banking.entity.status.Role;
import java.time.Instant;
import java.util.UUID;

/** Never exposes the password hash or other internal fields of {@link User}. */
public record UserResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        Instant createdAt
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
