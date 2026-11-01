package io.github.vitaliyprogrammer.banking.cashback.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import io.github.vitaliyprogrammer.banking.cashback.configuration.CashbackProperties;
import io.github.vitaliyprogrammer.banking.cashback.dto.response.CashbackResponseDto;
import io.github.vitaliyprogrammer.banking.cashback.entity.CashbackAccrual;
import io.github.vitaliyprogrammer.banking.cashback.repository.CashbackAccountTotal;
import io.github.vitaliyprogrammer.banking.cashback.repository.CashbackAccrualRepository;
import io.github.vitaliyprogrammer.banking.cashback.repository.CashbackPayoutRepository;
import io.github.vitaliyprogrammer.banking.cashback.service.CashbackService;
import io.github.vitaliyprogrammer.banking.payment.event.BillPaymentCompletedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cashback in two steps, like interest: earn on every completed bill payment (capped per month), pay once a month.
 * The payment module never calls this class: it only announces {@link BillPaymentCompletedEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackServiceImpl implements CashbackService {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CashbackAccrualRepository cashbackAccrualRepository;

    private final CashbackPayoutRepository cashbackPayoutRepository;

    private final AccountService accountService;

    private final LedgerService ledgerService;

    private final CashbackProperties properties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    /**
     * BEFORE_COMMIT: runs inside the payment's own transaction, right before it commits. The cashback row and the
     * completed payment are saved together or not at all, so cashback is never earned for a payment that rolled back.
     * The cap is UAH, so only UAH payments earn cashback (all providers today bill in UAH).
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBillPaymentCompleted(BillPaymentCompletedEvent event) {

        BigDecimal rate = properties.rate(event.category());
        if (rate.signum() <= 0 || event.currency() != Currency.UAH) {
            return;
        }

        String period = YearMonth.from(event.completedAt().atZone(KYIV)).toString();
        cashbackAccrualRepository.lockForUser("cashback:" + event.userId());

        if (cashbackAccrualRepository.existsByPaymentId(event.paymentId())) {
            return;
        }

        BigDecimal remaining = properties.monthlyCap()
                .subtract(cashbackAccrualRepository.sumForUser(event.userId(), period));
        BigDecimal amount = event.amount().multiply(rate).divide(HUNDRED, 2, RoundingMode.FLOOR).min(remaining);

        if (amount.signum() > 0) {
            cashbackAccrualRepository.save(CashbackAccrual.earn(event.userId(), event.paymentId(), event.accountId(),
                    event.category(), event.amount(), rate, amount, period, clock.instant()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CashbackResponseDto getCashback(UUID userId, YearMonth period) {

        List<CashbackAccrual> accruals = cashbackAccrualRepository.findAllByUserIdAndPeriodOrderByCreatedAt(
                userId, period.toString());
        BigDecimal earned = accruals.stream().map(CashbackAccrual::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean paidOut = accruals.stream()
                .map(CashbackAccrual::getAccountId)
                .distinct()
                .anyMatch(accountId -> cashbackPayoutRepository.existsByAccountIdAndPeriod(accountId, period.toString()));

        return new CashbackResponseDto(
                period.toString(),
                earned.setScale(2),
                properties.monthlyCap(),
                properties.monthlyCap().subtract(earned).max(BigDecimal.ZERO).setScale(2),
                paidOut,
                properties.rates(),
                accruals.stream().map(CashbackResponseDto.Item::from).toList());
    }

    /** One short transaction per account; the payout row goes in first, exactly as with savings interest. */
    @Override
    public int payCashback(YearMonth period) {

        int paid = 0;

        for (CashbackAccountTotal total : cashbackAccrualRepository.totalsByAccount(period.toString())) {
            if (Boolean.TRUE.equals(transactionTemplate.execute(_ -> payOne(total, period)))) {
                paid++;
            }
        }

        log.info("Cashback paid for {}: {} accounts", period, paid);
        return paid;
    }

    private boolean payOne(CashbackAccountTotal total, YearMonth period) {

        if (cashbackPayoutRepository.insertIfAbsent(total.getAccountId(), period.toString(), total.getTotal(),
                clock.instant()) == 0) {
            return false;
        }

        AccountReferenceDto account = accountService.getAccountReference(total.getAccountId());
        UUID ledgerTransactionId = ledgerService.post(PostingRequestDto.transfer(
                TransactionType.CASHBACK,
                "Кешбек за " + period,
                ledgerService.getBankAccountId(AccountType.CASHBACK_EXPENSE, account.currency()),
                account.id(),
                total.getTotal()));

        cashbackPayoutRepository.findByAccountIdAndPeriod(account.id(), period.toString())
                .orElseThrow()
                .attachLedgerTransaction(ledgerTransactionId);

        return true;
    }
}
