package io.github.vitaliyprogrammer.banking.identity.controller;

import io.github.vitaliyprogrammer.banking.identity.dto.request.LoginRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RefreshRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RegisterRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.TokenResponseDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.identity.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login and session management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new client",
            description = "Creates an active client account. Staff roles cannot be self-assigned")
    public UserResponseDto register(@Valid @RequestBody RegisterRequestDto request) {

        return authenticationService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Log in",
            description = "Exchanges email and password for an access token and a refresh token")
    public TokenResponseDto login(@Valid @RequestBody LoginRequestDto request) {

        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refresh tokens",
            description = "Rotates the refresh token: the old one is revoked, a new pair is returned. "
                    + "Reusing a revoked token revokes all sessions of the user")
    public TokenResponseDto refresh(@Valid @RequestBody RefreshRequestDto request) {

        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out",
            description = "Revokes the refresh token. Logging out twice is not an error")
    public void logout(@Valid @RequestBody RefreshRequestDto request) {

        authenticationService.logout(request);
    }
}
