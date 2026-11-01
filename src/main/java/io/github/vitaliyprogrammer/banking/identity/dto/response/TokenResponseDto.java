package io.github.vitaliyprogrammer.banking.identity.dto.response;

public record TokenResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    public static TokenResponseDto bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponseDto(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
