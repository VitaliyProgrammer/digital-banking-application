package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.LoginRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.RefreshRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.RegisterRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.TokenResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;

public interface AuthenticationService {

    UserResponseDto register(RegisterRequestDto request);

    TokenResponseDto login(LoginRequestDto request);

    TokenResponseDto refresh(RefreshRequestDto request);

    void logout(RefreshRequestDto request);
}
