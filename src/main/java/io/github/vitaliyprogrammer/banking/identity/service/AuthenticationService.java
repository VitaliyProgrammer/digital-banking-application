package io.github.vitaliyprogrammer.banking.identity.service;

import io.github.vitaliyprogrammer.banking.identity.dto.request.LoginRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RefreshRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.request.RegisterRequestDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.TokenResponseDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;

public interface AuthenticationService {

    UserResponseDto register(RegisterRequestDto request);

    TokenResponseDto login(LoginRequestDto request);

    TokenResponseDto refresh(RefreshRequestDto request);

    void logout(RefreshRequestDto request);
}
