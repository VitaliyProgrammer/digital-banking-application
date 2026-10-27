package io.github.vitaliyprogrammer.banking.account.service;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.shared.PageResponseDto;
import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto open(UUID ownerId, OpenAccountRequestDto request);

    List<AccountResponseDto> getAccounts(UUID ownerId);

    AccountResponseDto getAccount(UUID ownerId, UUID accountId);

    PageResponseDto<StatementEntryResponseDto> getStatement(UUID ownerId, UUID accountId, int page, int size);
}
