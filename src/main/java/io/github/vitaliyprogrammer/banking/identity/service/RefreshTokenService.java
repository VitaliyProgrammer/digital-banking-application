package io.github.vitaliyprogrammer.banking.identity.service;

import java.util.UUID;

public interface RefreshTokenService {

    String issue(UUID userId);

    UUID consume(String rawToken);

    void revoke(String rawToken);
}
