package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditStatementResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.SettlementDto;
import io.github.vitaliyprogrammer.banking.entity.CreditStatement;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.repository.CreditStatementRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.service.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The account holds the limit and the balance, the ledger holds the history. A statement is a snapshot of that
 * history at the end of a month, so the bank can later prove what was owed, by when, and what it charged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLineServiceImpl implements CreditLineService {

    private final AccountService accountService;

    private final CardService cardService;

    private final LedgerService ledgerService;

    private final CreditStatementRepository creditStatementRepository;

    private final CreditTerms creditTerms;

    private final TransactionTemplate transactionTemplate;

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
     * Read from the ledger with the same formula the monthly job uses, so it is right even before the job has run.
     */
    @Override
    @Transactional(readOnly = true)
    public CreditLineResponseDto getCreditLine(UUID ownerId, LocalDate today) {

        AccountResponseDto account = accountService.getCreditAccount(ownerId);

        Instant statementClosedAt = GracePeriod.lastStatementClosedAt(today).toInstant();

        BigDecimal statementDebt = debt(accountService.getBalanceAt(account.id(), statementClosedAt));
        BigDecimal repaidSince = accountService.getCreditedSince(account.id(), statementClosedAt);
        BigDecimal minimumPayment = creditTerms.minimumPayment(statementDebt);

        return new CreditLineResponseDto(
                account.id(),
                account.number(),
                account.creditLimit(),
                money(account.creditLimit().add(account.balance()).max(BigDecimal.ZERO)),
                debt(account.balance()),
                statementDebt,
                leftOf(statementDebt, repaidSince),
                minimumPayment,
                leftOf(minimumPayment, repaidSince),
                GracePeriod.interestFreeUntil(today.minusMonths(1)),
                GracePeriod.interestFreeUntil(today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditStatementResponseDto> getStatements(UUID ownerId) {

        UUID accountId = accountService.getCreditAccountReference(ownerId).id();

        return creditStatementRepository.findAllByAccountIdOrderByClosedAtDesc(accountId).stream()
                .map(CreditStatementResponseDto::from)
                .toList();
    }

    /**
     * Two short transactions per account, so a problem with one credit line does not stop everyone else's.
     * Settling first or closing first makes no difference: charges posted now belong to this month, not the last.
     */
    @Override
    public int closeMonth(LocalDate today) {

        Instant moment = today.atStartOfDay(GracePeriod.KYIV).toInstant();
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        int closed = 0;

        for (AccountReferenceDto account : accountService.getActiveCreditAccounts()) {
            transactionTemplate.executeWithoutResult(_ -> settleDue(account, moment));

            if (Boolean.TRUE.equals(transactionTemplate.execute(_ -> closeStatement(account, lastMonth)))) {
                closed++;
            }
        }

        log.info("Credit statements for {}: {} closed", lastMonth, closed);
        return closed;
    }

    /**
     * Only repayments inside the statement's own window count: from its closing to its due date. That window is the
     * month after it, so two statements never count the same repayment.
     */
    private void settleDue(AccountReferenceDto account, Instant moment) {

        for (CreditStatement statement : creditStatementRepository.findDueForUpdate(account.id(), moment)) {
            BigDecimal repaid = money(accountService.getCreditedBetween(
                    account.id(), statement.getClosedAt(), statement.getDueAt()));
            SettlementDto settlement = creditTerms.settle(
                    statement.getClosingDebt(), statement.getMinimumPayment(), repaid);

            UUID ledgerTransactionId = settlement.charges().signum() > 0
                    ? charge(account, statement.getPeriod(), settlement)
                    : null;

            statement.settle(repaid, settlement, ledgerTransactionId, clock.instant());
            log.info("Credit statement {} of {}: {}, repaid {}, interest {}, late fee {}", statement.getPeriod(),
                    account.id(), settlement.status(), repaid, settlement.interest(), settlement.lateFee());
        }
    }

    /** One ledger transaction: the debt grows by both charges, and each goes to its own income account. */
    private UUID charge(AccountReferenceDto account, String period, SettlementDto settlement) {

        List<PostingRequestDto.Line> lines = new ArrayList<>();
        lines.add(new PostingRequestDto.Line(account.id(), EntryDirection.DEBIT, settlement.charges()));

        if (settlement.interest().signum() > 0) {
            lines.add(new PostingRequestDto.Line(
                    ledgerService.getBankAccountId(AccountType.INTEREST_INCOME, Currency.UAH),
                    EntryDirection.CREDIT, settlement.interest()));
        }
        if (settlement.lateFee().signum() > 0) {
            lines.add(new PostingRequestDto.Line(
                    ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH),
                    EntryDirection.CREDIT, settlement.lateFee()));
        }

        String description = settlement.lateFee().signum() > 0
                ? "Відсотки та штраф за прострочку, виписка " + period
                : "Відсотки за кредитом, виписка " + period;

        return ledgerService.post(new PostingRequestDto(TransactionType.CREDIT_CHARGES, description, lines));
    }

    /** Nothing owed at the end of the month (or own money on the account): no statement at all. */
    private boolean closeStatement(AccountReferenceDto account, YearMonth period) {

        Instant closedAt = GracePeriod.statementClosedAt(period);
        BigDecimal closingDebt = debt(accountService.getBalanceAt(account.id(), closedAt));

        if (closingDebt.signum() == 0) {
            return false;
        }

        return creditStatementRepository.insertIfAbsent(account.id(), period.toString(), closingDebt,
                creditTerms.minimumPayment(closingDebt), closedAt, GracePeriod.statementDueDate(period),
                GracePeriod.statementDueAt(period), clock.instant()) == 1;
    }

    /** A negative balance is debt; a positive one is the client's own money, so no debt at all. */
    private static BigDecimal debt(BigDecimal balance) {

        return money(balance.negate().max(BigDecimal.ZERO));
    }

    private static BigDecimal leftOf(BigDecimal due, BigDecimal repaid) {

        return money(due.subtract(repaid).max(BigDecimal.ZERO));
    }

    private static BigDecimal money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
