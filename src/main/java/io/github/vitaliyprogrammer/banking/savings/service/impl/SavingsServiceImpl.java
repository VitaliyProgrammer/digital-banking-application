package io.github.vitaliyprogrammer.banking.savings.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import io.github.vitaliyprogrammer.banking.savings.configuration.SavingsProperties;
import io.github.vitaliyprogrammer.banking.savings.dto.response.SavingsResponseDto;
import io.github.vitaliyprogrammer.banking.savings.repository.InterestAccrualRepository;
import io.github.vitaliyprogrammer.banking.savings.repository.InterestPayoutRepository;
import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Interest in two steps, like in a real bank:
 * 1. every day, accrue: the balance at the end of the day × annual rate / 365, kept with 8 decimals outside the ledger;
 * 2. once a month, pay: everything accrued and not paid yet, rounded DOWN to kopiyky, as one ledger transaction.
 * The fraction left after rounding is not lost: it stays "accrued, not paid" and joins next month's payout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsServiceImpl implements SavingsService {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    /** Simple ACT/365 convention: every day earns 1/365 of the annual rate, leap years included. */
    private static final BigDecimal DAYS_IN_YEAR_TIMES_100 = BigDecimal.valueOf(36_500);

    private static final int ACCRUAL_SCALE = 8;

    private final InterestAccrualRepository interestAccrualRepository;

    private final InterestPayoutRepository interestPayoutRepository;

    private final AccountService accountService;

    private final LedgerService ledgerService;

    private final SavingsProperties properties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Override
    @Transactional
    public SavingsResponseDto open(UUID userId, OpenAccountRequestDto request) {

        return toResponse(accountService.openSavings(userId, request.currency()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsResponseDto> getSavings(UUID userId) {

        return accountService.getAccounts(userId).stream()
                .filter(account -> account.type() == AccountType.SAVINGS)
                .map(this::toResponse)
                .toList();
    }

    /**
     * The balance is read from the ledger as it was at midnight in Kyiv, not as it is now: a job that runs late,
     * or catches up a missed day, still pays exactly for what was in the jar that day.
     */
    @Override
    @Transactional
    public int accrueInterest(LocalDate day) {

        Instant endOfDay = day.plusDays(1).atStartOfDay(KYIV).toInstant();
        int accrued = 0;

        for (AccountReferenceDto account : accountService.getActiveSavingsAccounts()) {
            BigDecimal balance = accountService.getBalanceAt(account.id(), endOfDay);
            BigDecimal rate = properties.annualRate(account.currency());
            BigDecimal interest = balance.multiply(rate).divide(DAYS_IN_YEAR_TIMES_100, ACCRUAL_SCALE, RoundingMode.FLOOR);

            if (interest.signum() > 0) {
                accrued += interestAccrualRepository.insertIfAbsent(
                        account.id(), day, balance, rate, interest, clock.instant());
            }
        }

        log.info("Interest accrued for {}: {} accounts", day, accrued);
        return accrued;
    }

    /** One short transaction per account, so a problem with one jar does not stop everyone else's payout. */
    @Override
    public int payInterest(YearMonth period) {

        int paid = 0;

        for (AccountReferenceDto account : accountService.getActiveSavingsAccounts()) {
            if (Boolean.TRUE.equals(transactionTemplate.execute(_ -> payOne(account, period)))) {
                paid++;
            }
        }

        log.info("Interest paid for {}: {} accounts", period, paid);
        return paid;
    }

    private boolean payOne(AccountReferenceDto account, YearMonth period) {

        BigDecimal unpaid = interestAccrualRepository.sumUntil(account.id(), period.atEndOfMonth())
                .subtract(interestPayoutRepository.sumPaid(account.id()));
        BigDecimal amount = unpaid.setScale(2, RoundingMode.FLOOR);

        if (amount.signum() <= 0) {
            return false;
        }

        // The payout row goes in first: if it is already there, this month was paid and no money moves.
        if (interestPayoutRepository.insertIfAbsent(account.id(), period.toString(), amount, clock.instant()) == 0) {
            return false;
        }

        UUID ledgerTransactionId = ledgerService.post(PostingRequestDto.transfer(
                TransactionType.INTEREST,
                "Відсотки за " + period,
                ledgerService.getBankAccountId(AccountType.INTEREST_EXPENSE, account.currency()),
                account.id(),
                amount));

        interestPayoutRepository.findByAccountIdAndPeriod(account.id(), period.toString())
                .orElseThrow()
                .attachLedgerTransaction(ledgerTransactionId);

        return true;
    }

    private SavingsResponseDto toResponse(AccountResponseDto account) {

        BigDecimal unpaid = interestAccrualRepository.sumAll(account.id())
                .subtract(interestPayoutRepository.sumPaid(account.id()));

        return new SavingsResponseDto(
                account.id(),
                account.number(),
                account.currency(),
                account.balance(),
                properties.annualRate(account.currency()),
                unpaid.setScale(2, RoundingMode.FLOOR),
                account.createdAt());
    }
}
