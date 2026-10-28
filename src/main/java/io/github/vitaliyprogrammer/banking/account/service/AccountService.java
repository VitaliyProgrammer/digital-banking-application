package io.github.vitaliyprogrammer.banking.account.service;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.shared.PageResponseDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("api")
public interface AccountService {

    AccountResponseDto open(UUID ownerId, OpenAccountRequestDto request);

    List<AccountResponseDto> getAccounts(UUID ownerId);

    AccountResponseDto getAccount(UUID ownerId, UUID accountId);

    PageResponseDto<StatementEntryResponseDto> getStatement(UUID ownerId, UUID accountId, int page, int size);

    /** For other modules: an account of the given owner, or 404 if it does not exist or belongs to someone else. */
    AccountReferenceDto getOwnedAccountReference(UUID ownerId, UUID accountId);

    /** For other modules: a client account by its IBAN, e.g. the recipient of a transfer. */
    Optional<AccountReferenceDto> findClientAccountByNumber(String number);

    /** For other modules: any client account by id, e.g. the account behind a recipient's card. */
    AccountReferenceDto getAccountReference(UUID accountId);
}
