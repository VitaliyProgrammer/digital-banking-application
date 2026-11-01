package io.github.vitaliyprogrammer.banking.savings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.savings.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.savings.dto.response.SavingsResponseDto;
import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 7.1. Money enters and leaves a jar only through the client's own current account,
 * interest is calculated on the balance as it was at the end of each day, every day and every month is
 * processed once however often the jobs run, and fractions of a kopiyka are carried over, not lost.
 * UAH earns 12% a year in the test configuration, so 36 500 UAH earns exactly 12.00 UAH a day.
 */
class SavingsIntegrationTests extends IntegrationTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    @Autowired
    private SavingsService savingsService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    private final LocalDate today = LocalDate.now(KYIV);

    private UUID clientId;

    private UUID currentAccount;

    private UUID jar;

    @BeforeEach
    void clientWithMoneyAndAnEmptyJar() {
        clientId = createUser();
        currentAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(currentAccount, new BigDecimal("50000.00")));
        jar = savingsService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
    }

    @Test
    void moneyEntersAndLeavesAJarOnlyThroughTheOwnCurrentAccount() {
        deposit("1000.00");
        paymentService.withdrawFromSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(currentAccount, new BigDecimal("300.00")));

        assertThat(balanceOf(jar)).isEqualByComparingTo("700.00");
        assertThat(balanceOf(currentAccount)).isEqualByComparingTo("49300.00");

        // Every other operation looks for a CURRENT account, so the jar is simply not there for it.
        UUID stranger = createUser();
        String strangerIban = accountService.open(stranger, new OpenAccountRequestDto(Currency.UAH)).number();
        assertThatThrownBy(() -> paymentService.transfer(clientId, newKey(),
                new TransferRequestDto(jar, strangerIban, new BigDecimal("10.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);
        // A jar can never go below zero either.
        assertThatThrownBy(() -> paymentService.withdrawFromSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(currentAccount, new BigDecimal("700.01"))))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void interestIsCalculatedOnTheBalanceAtTheEndOfEachDay() {
        deposit("36500.00");

        // Yesterday ended before the money arrived: nothing to pay for, even though the job runs today.
        savingsService.accrueInterest(today.minusDays(1));
        assertThat(jarInfo().accruedInterest()).isEqualByComparingTo("0.00");

        savingsService.accrueInterest(today);
        assertThat(jarInfo().accruedInterest()).isEqualByComparingTo("12.00");
    }

    @Test
    void aDayIsAccruedOnceAndAMonthIsPaidOnceHoweverOftenTheJobsRun() {
        deposit("36500.00");

        savingsService.accrueInterest(today);
        savingsService.accrueInterest(today);
        savingsService.payInterest(YearMonth.from(today));
        savingsService.payInterest(YearMonth.from(today));

        assertThat(balanceOf(jar)).isEqualByComparingTo("36512.00");
        assertThat(jarInfo().accruedInterest()).isEqualByComparingTo("0.00");
    }

    @Test
    void fractionsOfAKopiykaAreCarriedOverToTheNextPayoutNotLost() {
        deposit("1000.00");

        // 1000 × 12% / 365 = 0.32876712: 0.32 is paid now, 0.00876712 waits for the next month.
        savingsService.accrueInterest(today);
        savingsService.payInterest(YearMonth.from(today));
        assertThat(balanceOf(jar)).isEqualByComparingTo("1000.32");

        // Next month: 1000.32 × 12% / 365 = 0.32887232, plus the carried 0.00876712 = 0.33763944: 0.33 is paid.
        LocalDate nextMonth = today.plusMonths(1);
        savingsService.accrueInterest(nextMonth);
        savingsService.payInterest(YearMonth.from(nextMonth));
        assertThat(balanceOf(jar)).isEqualByComparingTo("1000.65");
    }

    private void deposit(String amount) {
        paymentService.depositToSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(currentAccount, new BigDecimal(amount)));
    }

    private SavingsResponseDto jarInfo() {
        return savingsService.getSavings(clientId).getFirst();
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
