package io.github.vitaliyprogrammer.banking.account.controller;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.shared.CurrentUserId;
import io.github.vitaliyprogrammer.banking.shared.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.shared.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Client accounts, balances and statements")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open an account",
            description = "Opens a client account in the given currency. One account per currency")
    public AccountResponseDto openAccount(@CurrentUserId UUID userId,
                                          @Valid @RequestBody OpenAccountRequestDto request) {

        return accountService.open(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my accounts",
            description = "All accounts of the authenticated client with current balances")
    public List<AccountResponseDto> getMyAccounts(@CurrentUserId UUID userId) {

        return accountService.getAccounts(userId);
    }

    @GetMapping("/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get account",
            description = "One account of the authenticated client. Someone else's account returns 404")
    public AccountResponseDto getAccount(@CurrentUserId UUID userId,
                                         @PathVariable UUID accountId) {

        return accountService.getAccount(userId, accountId);
    }

    @GetMapping("/{accountId}/transactions")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get account statement",
            description = "Ledger entries of the account, newest first, with the balance after each operation")
    public PageResponseDto<StatementEntryResponseDto> getStatement(
            @CurrentUserId UUID userId,
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return accountService.getStatement(userId, accountId, page, size);
    }
}
