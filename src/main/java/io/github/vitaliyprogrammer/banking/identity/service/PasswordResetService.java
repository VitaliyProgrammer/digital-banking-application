package io.github.vitaliyprogrammer.banking.identity.service;

import io.github.vitaliyprogrammer.banking.identity.dto.request.PasswordResetConfirmRequestDto;

public interface PasswordResetService {

    /**
     * Emails a one-time link if the address belongs to an active client. Returns the same way whether it does or not,
     * so the answer never tells who has an account.
     */
    void requestReset(String email);

    /** Sets the new password, uses up the link and ends every session of the user. */
    void resetPassword(PasswordResetConfirmRequestDto request);
}
