package io.github.vitaliyprogrammer.banking.identity.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile of the authenticated user")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    /** The user id comes from the verified token, never from the URL: a client can only read their own profile. */
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get current user profile",
            description = "Retrieve profile of the currently authenticated user")
    public UserResponseDto getCurrentUserProfile(@CurrentUserId UUID userId) {

        return userService.getById(userId);
    }
}
