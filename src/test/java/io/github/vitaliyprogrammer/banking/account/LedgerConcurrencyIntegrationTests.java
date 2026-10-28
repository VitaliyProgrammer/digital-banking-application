package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.LedgerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proof tests for phase 1: real parallel transactions on a real PostgreSQL. */
class LedgerConcurrencyIntegrationTests extends IntegrationTest {

    private static final int THREADS = 50;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void parallelDebitsNeverOverdrawAnAccount() throws Exception {
        UUID payer = openFundedAccount("100.00");
        UUID receiver = openFundedAccount("0.00");

        // 50 attempts to send 10.00 from an account holding 100.00, all released at the same moment.
        List<Result> results = runConcurrently(THREADS, i -> () -> transfer(payer, receiver, "10.00"));

        assertThat(results).filteredOn(Result.SUCCESS::equals).hasSize(10);
        assertThat(results).filteredOn(Result.INSUFFICIENT_FUNDS::equals).hasSize(40);
        assertThat(balanceOf(payer)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(receiver)).isEqualByComparingTo("100.00");
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        UUID first = openFundedAccount("1000.00");
        UUID second = openFundedAccount("1000.00");

        // Half of the threads send first → second, the other half second → first, at the same time.
        // Locking the two accounts in request order would deadlock here; locking by id cannot.
        List<Result> results = runConcurrently(THREADS, i -> i % 2 == 0
                ? () -> transfer(first, second, "1.00")
                : () -> transfer(second, first, "1.00"));

        assertThat(results).containsOnly(Result.SUCCESS);
        assertThat(balanceOf(first).add(balanceOf(second))).isEqualByComparingTo("2000.00");
    }

    @Test
    void databaseRejectsAnyChangeToLedgerHistory() {
        openFundedAccount("50.00");

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE ledger_entries SET amount = amount * 2"))
                .hasMessageContaining("Ledger is append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM ledger_transactions"))
                .hasMessageContaining("Ledger is append-only");
    }

    private UUID openFundedAccount(String amount) {
        UUID accountId = accountService.open(createUser(), new OpenAccountRequestDto(Currency.UAH)).id();

        BigDecimal initial = new BigDecimal(amount);
        if (initial.signum() > 0) {
            ledgerService.post(PostingRequestDto.transfer(TransactionType.DEPOSIT, "Initial deposit",
                    ledgerService.getBankAccountId(AccountType.SETTLEMENT, Currency.UAH), accountId, initial));
        }
        return accountId;
    }

    private Result transfer(UUID from, UUID to, String amount) {
        try {
            ledgerService.post(PostingRequestDto.transfer(TransactionType.TRANSFER, "Test transfer",
                    from, to, new BigDecimal(amount)));
            return Result.SUCCESS;
        } catch (InsufficientFundsException e) {
            return Result.INSUFFICIENT_FUNDS;
        }
    }

    private enum Result {
        SUCCESS,
        INSUFFICIENT_FUNDS
    }
}
