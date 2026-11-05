package io.github.vitaliyprogrammer.banking.tax.service.impl;

import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.business.dto.response.BusinessTaxProfileDto;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.outbox.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.outbox.service.OutboxService;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.tax.configuration.TaxProperties;
import io.github.vitaliyprogrammer.banking.tax.dto.response.TaxPeriodResponseDto;
import io.github.vitaliyprogrammer.banking.tax.entity.TaxObligation;
import io.github.vitaliyprogrammer.banking.tax.entity.status.TaxNoticeKind;
import io.github.vitaliyprogrammer.banking.tax.entity.status.TaxPeriodStatus;
import io.github.vitaliyprogrammer.banking.tax.event.TaxNoticeMessage;
import io.github.vitaliyprogrammer.banking.tax.exception.NothingToPayException;
import io.github.vitaliyprogrammer.banking.tax.exception.TaxPeriodNotFoundException;
import io.github.vitaliyprogrammer.banking.tax.repository.TaxObligationRepository;
import io.github.vitaliyprogrammer.banking.tax.service.TaxService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The single tax of sole proprietors. What is owed is worked out every time from the tax group and the income;
 * only what happened (paid so far, notices sent) is stored, per period, and changed under that row's lock.
 * The bank pays on its own only with the client's consent (auto-pay), in full or not at all.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private final TaxObligationRepository taxObligationRepository;

    private final BusinessService businessService;

    private final AccountService accountService;

    private final PaymentService paymentService;

    private final OutboxService outboxService;

    private final TaxProperties properties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TaxPeriodResponseDto> getPeriods(UUID ownerId) {

        BusinessTaxProfileDto business = businessService.getTaxProfile(ownerId);
        LocalDate today = today();
        Map<String, TaxObligation> obligations = taxObligationRepository.findAllByBusinessId(business.businessId())
                .stream().collect(Collectors.toMap(TaxObligation::getPeriod, Function.identity()));

        return periods(business, today).stream()
                .sorted(Comparator.comparing(TaxPeriod::from).reversed())
                .map(period -> describe(business, period, obligations.get(period.code()), today))
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponseDto payNow(UUID ownerId, String period) {

        BusinessTaxProfileDto business = businessService.getTaxProfile(ownerId);
        TaxPeriod taxPeriod = periods(business, today()).stream()
                .filter(candidate -> candidate.code().equals(period))
                .findFirst()
                .orElseThrow(TaxPeriodNotFoundException::new);

        return pay(business, taxPeriod).orElseThrow(NothingToPayException::new);
    }

    /**
     * One short transaction per sole proprietor, so a problem with one of them does not stop the others,
     * and a payment that fails for one never undoes a reminder sent to another.
     */
    @Override
    public int runDaily(LocalDate today) {

        int paid = 0;
        for (BusinessTaxProfileDto business : businessService.getTaxProfiles()) {
            Integer paidHere = transactionTemplate.execute(_ -> runFor(business, today));
            paid += paidHere == null ? 0 : paidHere;
        }
        log.info("Tax job for {}: {} periods paid", today, paid);
        return paid;
    }

    private int runFor(BusinessTaxProfileDto business, LocalDate today) {

        int paid = 0;
        for (TaxPeriod period : periods(business, today)) {
            // A group 3 quarter still going on is not due yet: its tax is still growing. A month of groups 1 and 2
            // is paid in advance, by the 20th of that same month, so the current month counts.
            if ((period.isQuarter() && period.isOpenOn(today)) || amountOf(business, period).signum() == 0) {
                continue;
            }
            taxObligationRepository.createIfMissing(business.businessId(), period.code());
            TaxObligation obligation = taxObligationRepository.findForUpdate(business.businessId(), period.code())
                    .orElseThrow();
            BigDecimal left = left(business, period, obligation);
            if (left.signum() == 0) {
                continue;
            }

            boolean dueSoon = !today.isAfter(period.dueDate())
                    && !today.isBefore(period.dueDate().minusDays(properties.reminderDaysBefore()));
            if (dueSoon && obligation.getReminderSentAt() == null) {
                notice(business, period, obligation, TaxNoticeKind.DUE_SOON, left);
                obligation.reminded(clock.instant());
            }

            if (business.autoPayTax() && !today.isBefore(period.dueDate())) {
                if (pay(business, period).isPresent()) {
                    paid++;
                } else if (obligation.getShortfallNotifiedAt() == null) {
                    notice(business, period, obligation, TaxNoticeKind.SHORTFALL, left);
                    obligation.shortfallNotified(clock.instant());
                }
            }
        }
        return paid;
    }

    /**
     * Under the period's row lock: what is left, from where, and the payment, with an idempotency key made of the
     * period and what was paid before, so even a retry that slipped past the lock could not pay twice.
     * Empty if there is nothing to pay or not enough money anywhere: the bank never pays part of a tax.
     */
    private Optional<PaymentResponseDto> pay(BusinessTaxProfileDto business, TaxPeriod period) {

        taxObligationRepository.createIfMissing(business.businessId(), period.code());
        TaxObligation obligation = taxObligationRepository.findForUpdate(business.businessId(), period.code())
                .orElseThrow();
        BigDecimal left = left(business, period, obligation);
        if (left.signum() == 0) {
            return Optional.empty();
        }

        Optional<UUID> source = sourceFor(business, left);
        if (source.isEmpty()) {
            return Optional.empty();
        }

        PaymentResponseDto payment = paymentService.payTax(business.ownerId(),
                "tax:" + obligation.getId() + ":" + obligation.getPaidAmount().stripTrailingZeros().toPlainString(),
                source.get(), left, "Єдиний податок за " + period.label());
        obligation.paid(left, payment.id());
        log.info("Tax for {} of {} paid: {}", period.code(), business.businessId(), left);
        return Optional.of(payment);
    }

    /** The tax jar first, the business account if the jar is short; never both, never part of the amount. */
    private Optional<UUID> sourceFor(BusinessTaxProfileDto business, BigDecimal amount) {

        for (UUID accountId : List.of(business.taxReserveAccountId(), business.businessAccountId())) {
            if (accountService.getAccount(business.ownerId(), accountId).balance().compareTo(amount) >= 0) {
                return Optional.of(accountId);
            }
        }
        return Optional.empty();
    }

    private void notice(BusinessTaxProfileDto business, TaxPeriod period, TaxObligation obligation,
                        TaxNoticeKind kind, BigDecimal left) {

        outboxService.record(OutboxEventType.TAX_NOTICE, obligation.getId(), new TaxNoticeMessage(
                business.ownerId(), kind, period.label(), left, period.dueDate(), business.autoPayTax()));
    }

    private TaxPeriodResponseDto describe(BusinessTaxProfileDto business, TaxPeriod period, TaxObligation obligation,
                                          LocalDate today) {

        BigDecimal amount = amountOf(business, period);
        BigDecimal paid = obligation == null ? money(BigDecimal.ZERO) : money(obligation.getPaidAmount());
        BigDecimal left = amount.subtract(paid).max(BigDecimal.ZERO);

        TaxPeriodStatus status;
        if (amount.signum() == 0 && !period.isOpenOn(today)) {
            status = TaxPeriodStatus.NOTHING_DUE;
        } else if (period.isQuarter() && period.isOpenOn(today)) {
            status = TaxPeriodStatus.ACCRUING;
        } else if (left.signum() == 0) {
            status = TaxPeriodStatus.PAID;
        } else {
            status = today.isAfter(period.dueDate()) ? TaxPeriodStatus.OVERDUE : TaxPeriodStatus.OPEN;
        }

        return new TaxPeriodResponseDto(period.code(), period.label(), period.dueDate(), amount, paid, left, status);
    }

    private BigDecimal left(BusinessTaxProfileDto business, TaxPeriod period, TaxObligation obligation) {

        return amountOf(business, period).subtract(obligation.getPaidAmount()).max(BigDecimal.ZERO);
    }

    private BigDecimal amountOf(BusinessTaxProfileDto business, TaxPeriod period) {

        return money(business.taxGroup().paysShareOfIncome()
                ? businessService.getTaxPutAside(business.businessId(), period.from(), period.to())
                : business.monthlyTax());
    }

    private static List<TaxPeriod> periods(BusinessTaxProfileDto business, LocalDate today) {

        return TaxPeriod.since(business.taxGroup(), business.openedAt().atZone(TaxPeriod.KYIV).toLocalDate(), today);
    }

    private LocalDate today() {

        return LocalDate.now(clock.withZone(TaxPeriod.KYIV));
    }

    private static BigDecimal money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
