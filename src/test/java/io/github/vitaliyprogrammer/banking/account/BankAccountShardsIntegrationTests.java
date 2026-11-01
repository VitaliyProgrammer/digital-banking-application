package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hot bank accounts are split into shards (V22), so parallel payments lock different rows. Found with k6:
 * every transfer used to queue behind the bank's one fee income row.
 */
class BankAccountShardsIntegrationTests extends IntegrationTest {

    @Autowired
    private LedgerService ledgerService;

    @Test
    void paymentsAreSpreadOverEveryShardOfAHotAccount() {
        Set<UUID> used = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            used.add(ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH));
        }

        Integer shards = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM accounts WHERE type = 'FEE_INCOME' AND currency = 'UAH'", Integer.class);
        assertThat(shards).isEqualTo(16);
        // With 1 000 draws the chance that a shard of 16 is never picked is about 16 · (15/16)^1000 ≈ 10^-27.
        assertThat(used).hasSize(16);
    }

    @Test
    void anAccountThatIsNotHotStaysOneRow() {
        UUID first = ledgerService.getBankAccountId(AccountType.INTEREST_EXPENSE, Currency.UAH);

        for (int i = 0; i < 100; i++) {
            assertThat(ledgerService.getBankAccountId(AccountType.INTEREST_EXPENSE, Currency.UAH)).isEqualTo(first);
        }
    }
}
