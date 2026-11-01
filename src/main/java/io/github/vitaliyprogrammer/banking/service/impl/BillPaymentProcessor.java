package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.BillPaymentProperties;
import io.github.vitaliyprogrammer.banking.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.ProviderPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderCallResult;
import io.github.vitaliyprogrammer.banking.entity.Payment;
import io.github.vitaliyprogrammer.banking.entity.Provider;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.event.BillPaymentCompletedEvent;
import io.github.vitaliyprogrammer.banking.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.event.PaymentMessage;
import io.github.vitaliyprogrammer.banking.repository.PaymentRepository;
import io.github.vitaliyprogrammer.banking.repository.ProviderRepository;
import io.github.vitaliyprogrammer.banking.service.LedgerService;
import io.github.vitaliyprogrammer.banking.service.OutboxService;
import io.github.vitaliyprogrammer.banking.service.ProviderClient;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Moves bill payments from PROCESSING / UNKNOWN to a final state. Every payment goes through three steps,
 * and the network call is deliberately NOT inside a database transaction:
 *
 * 1. claim   (short transaction) — lock due rows with SKIP LOCKED, lease them, commit.
 * 2. call    (no transaction)    — ask the provider; this may take seconds or time out.
 * 3. apply   (short transaction) — lock the row again and move the money according to the answer.
 *
 * Holding a row lock and a DB connection while waiting for a slow provider would block other work and
 * exhaust the connection pool; the lease (next_attempt_at) protects the payment between steps 1 and 3 instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillPaymentProcessor {

    private final PaymentRepository paymentRepository;

    private final ProviderRepository providerRepository;

    private final ProviderClient providerClient;

    private final LedgerService ledgerService;

    private final TransactionTemplate transactionTemplate;

    private final ApplicationEventPublisher eventPublisher;

    private final OutboxService outboxService;

    private final BusinessMetrics businessMetrics;

    private final BillPaymentProperties properties;

    private final Clock clock;

    /** Sends held payments to their providers. Returns how many payments this call took. */
    public int processPending() {

        List<UUID> claimed = claim(PaymentStatus.PROCESSING);

        for (UUID paymentId : claimed) {
            Payment payment = paymentRepository.findById(paymentId).orElseThrow();
            Provider provider = providerRepository.findById(payment.getProviderId()).orElseThrow();

            ProviderCallResult result = providerClient.send(provider.getCode(), new ProviderPaymentRequestDto(
                    payment.getId(), payment.getProviderAccountNumber(), payment.getAmount(), payment.getCurrency()));

            apply(paymentId, result);
        }

        return claimed.size();
    }

    /**
     * Reconciliation: for payments in UNKNOWN, ask the provider what actually happened instead of sending again.
     * Not every provider handles a repeated payment safely, but every one can answer "do you have payment X?".
     */
    public int reconcileUnknown() {

        List<UUID> claimed = claim(PaymentStatus.UNKNOWN);

        for (UUID paymentId : claimed) {
            Payment payment = paymentRepository.findById(paymentId).orElseThrow();
            Provider provider = providerRepository.findById(payment.getProviderId()).orElseThrow();

            apply(paymentId, providerClient.checkStatus(provider.getCode(), paymentId));
        }

        return claimed.size();
    }

    private List<UUID> claim(PaymentStatus status) {

        Instant now = clock.instant();

        List<UUID> claimed = transactionTemplate.execute(transaction -> {
            List<UUID> ids = new ArrayList<>();

            for (Payment payment : paymentRepository.lockDue(status.name(), now, properties.batchSize())) {
                payment.recordAttempt(now.plus(properties.lease()));
                ids.add(payment.getId());
            }

            return ids;
        });

        return Objects.requireNonNull(claimed);
    }

    /** Step 3: lock the row and react to the provider's answer. Every branch ends in a valid, saved state. */
    private void apply(UUID paymentId, ProviderCallResult result) {

        transactionTemplate.executeWithoutResult(transaction -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();

            if (payment.isFinished()) {
                return;
            }

            Instant now = clock.instant();

            switch (result.outcome()) {
                case ACCEPTED -> capture(payment, now);
                case REJECTED -> release(payment, rejectionReason(result), now);
                case NOT_FOUND -> release(payment, "Provider never received the payment", now);
                case NO_ANSWER -> payment.markUnknown(now.plus(properties.reconciliationDelay()));
                case NOT_DELIVERED -> {
                    // Certainly not sent, so trying again later is safe. The lease from the claim schedules it.
                    if (payment.getStatus() == PaymentStatus.PROCESSING
                            && payment.getAttempts() >= properties.maxDeliveryAttempts()) {
                        release(payment, "Provider unavailable", now);
                    }
                }
            }

            log.info("Bill payment {} → {} ({})", paymentId, payment.getStatus(), result.outcome());
        });
    }

    /** HOLD → settlement (amount leaves the bank) and HOLD → fee income (if any): one balanced transaction. */
    private void capture(Payment payment, Instant now) {

        UUID holdAccount = ledgerService.getBankAccountId(AccountType.HOLD, payment.getCurrency());
        BigDecimal held = payment.getAmount().add(payment.getFee());

        List<PostingRequestDto.Line> lines = new ArrayList<>();
        lines.add(new PostingRequestDto.Line(holdAccount, EntryDirection.DEBIT, held));
        lines.add(new PostingRequestDto.Line(
                ledgerService.getBankAccountId(AccountType.SETTLEMENT, payment.getCurrency()),
                EntryDirection.CREDIT, payment.getAmount()));

        if (payment.getFee().signum() > 0) {
            lines.add(new PostingRequestDto.Line(
                    ledgerService.getBankAccountId(AccountType.FEE_INCOME, payment.getCurrency()),
                    EntryDirection.CREDIT, payment.getFee()));
        }

        UUID captureId = ledgerService.post(new PostingRequestDto(TransactionType.CAPTURE, payment.getDescription(), lines));
        payment.complete(captureId, now);

        // Listeners run inside this transaction (BEFORE_COMMIT): what they write commits or rolls back with the payment.
        Provider provider = providerRepository.findById(payment.getProviderId()).orElseThrow();
        eventPublisher.publishEvent(new BillPaymentCompletedEvent(
                payment.getId(), payment.getInitiatorId(), payment.getFromAccountId(), provider.getCategory(),
                payment.getCurrency(), payment.getAmount(), now));
        outboxService.record(OutboxEventType.PAYMENT_COMPLETED, payment.getId(), PaymentMessage.from(payment));
        businessMetrics.paymentCompleted(payment.getType());
    }

    /** HOLD → client account: the full held amount comes back. */
    private void release(Payment payment, String reason, Instant now) {

        UUID releaseId = ledgerService.post(PostingRequestDto.transfer(
                TransactionType.RELEASE, "Returned: " + payment.getDescription(),
                ledgerService.getBankAccountId(AccountType.HOLD, payment.getCurrency()),
                payment.getFromAccountId(),
                payment.getAmount().add(payment.getFee())));

        payment.fail(releaseId, reason, now);
        outboxService.record(OutboxEventType.PAYMENT_FAILED, payment.getId(), PaymentMessage.from(payment));
        businessMetrics.paymentFailed(payment.getType());
    }

    private static String rejectionReason(ProviderCallResult result) {

        return result.detail() != null ? result.detail() : "Rejected by provider";
    }
}
