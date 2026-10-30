package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.CreditLineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The credit line has no table of its own: the account holds the limit and the balance, the ledger holds
 * the history, and the grace period is read from that history. Nothing to keep in sync, nothing to drift.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLineServiceImpl implements CreditLineService {

    private final AccountService accountService;

    private final CardService cardService;

    private final Clock clock;

    @Override
    @Transactional
    public void open(UUID ownerId, BigDecimal creditLimit) {

        AccountResponseDto account = accountService.openCreditAccount(ownerId, creditLimit);
        cardService.issueCreditCard(ownerId);

        log.info("Credit line {} opened for {} with limit {}", account.id(), ownerId, creditLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditLineResponseDto getCreditLine(UUID ownerId) {

        return getCreditLine(ownerId, LocalDate.now(clock.withZone(GracePeriod.KYIV)));
    }

    /**
     * Payments since the statement closed go towards that statement first: whatever was repaid after midnight
     * on the 1st reduces what is left of last month's debt. Spending this month belongs to the next statement.
     */
    @Override
    @Transactional(readOnly = true)
    public CreditLineResponseDto getCreditLine(UUID ownerId, LocalDate today) {

        AccountResponseDto account = accountService.getCreditAccount(ownerId);

        Instant statementClosedAt = GracePeriod.lastStatementClosedAt(today).toInstant();

        BigDecimal statementDebt = debt(accountService.getBalanceAt(account.id(), statementClosedAt));
        BigDecimal repaidSince = accountService.getCreditedSince(account.id(), statementClosedAt);

        return new CreditLineResponseDto(
                account.id(),
                account.number(),
                account.creditLimit(),
                account.creditLimit().add(account.balance()),
                debt(account.balance()),
                statementDebt,
                money(statementDebt.subtract(repaidSince).max(BigDecimal.ZERO)),
                GracePeriod.interestFreeUntil(today.minusMonths(1)),
                GracePeriod.interestFreeUntil(today));
    }

    /** A negative balance is debt; a positive one is the client's own money, so no debt at all. */
    private static BigDecimal debt(BigDecimal balance) {

        return money(balance.negate().max(BigDecimal.ZERO));
    }

    private static BigDecimal money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
