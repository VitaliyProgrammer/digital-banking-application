package io.github.vitaliyprogrammer.banking.identity.service;

import java.util.UUID;

public interface RefreshTokenService {

    String issue(UUID userId);

    UUID consume(String rawToken);

    void revoke(String rawToken);

    /** Ends every session of the user at once, e.g. after a password reset. */
    void revokeAllSessions(UUID userId);
}
