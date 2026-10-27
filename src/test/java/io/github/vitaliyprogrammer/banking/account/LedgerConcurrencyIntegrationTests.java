package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.TestcontainersConfiguration;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proof tests for phase 1. Every test hits a real PostgreSQL with real parallel transactions,
 * and after every test the whole ledger must still reconcile.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerConcurrencyIntegrationTests {

    private static final int THREADS = 50;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * Invariants that must hold after any sequence of operations:
     * the cached balance equals the sum of the account's entries, and money is never created or lost.
     */
    @AfterEach
    void ledgerReconciles() {
        Integer mismatchedAccounts = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM accounts a
                WHERE a.balance <> COALESCE((
                    SELECT sum(CASE e.direction WHEN 'CREDIT' THEN e.amount ELSE -e.amount END)
                    FROM ledger_entries e WHERE e.account_id = a.id), 0)
                """, Integer.class);
        assertThat(mismatchedAccounts).as("accounts whose balance differs from their ledger entries").isZero();

        List<BigDecimal> totalsByCurrency = jdbcTemplate.queryForList(
                "SELECT sum(balance) FROM accounts GROUP BY currency", BigDecimal.class);
        assertThat(totalsByCurrency).allSatisfy(total -> assertThat(total).isEqualByComparingTo("0"));
    }

    private UUID openFundedAccount(String amount) {
        UUID ownerId = createUser();
        UUID accountId = accountService.open(ownerId, new OpenAccountRequestDto(Currency.UAH)).id();

        BigDecimal initial = new BigDecimal(amount);
        if (initial.signum() > 0) {
            ledgerService.post(PostingRequestDto.transfer(TransactionType.DEPOSIT, "Initial deposit",
                    ledgerService.getSettlementAccountId(Currency.UAH), accountId, initial));
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

    /** Starts all tasks at the same instant (a start gate), so they really compete for the same rows. */
    private static List<Result> runConcurrently(int count, TaskFactory factory) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                Callable<Result> task = factory.create(i);
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();

            List<Result> results = new ArrayList<>();
            for (Future<Result> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }

    private BigDecimal balanceOf(UUID accountId) {
        return jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, created_at)
                VALUES (?, ?, 'not-used', 'Test', 'Client', 'CLIENT', 'ACTIVE', now())
                """, id, "client-" + id + "@example.com");
        return id;
    }

    private enum Result {
        SUCCESS,
        INSUFFICIENT_FUNDS
    }

    @FunctionalInterface
    private interface TaskFactory {
        Callable<Result> create(int index);
    }
}
