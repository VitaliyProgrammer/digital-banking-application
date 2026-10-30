package io.github.vitaliyprogrammer.banking.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.dto.response.NotificationDraftDto;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.CreditStatementStatus;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.DecisionSource;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.event.CreditDecisionMessage;
import io.github.vitaliyprogrammer.banking.event.CreditStatementMessage;
import io.github.vitaliyprogrammer.banking.event.PaymentMessage;
import io.github.vitaliyprogrammer.banking.service.impl.NotificationComposer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Texts and the "worth an email" rule, without Spring. */
class NotificationComposerTests {

    private static final UUID USER = UUID.randomUUID();

    @Test
    void everydayPaymentsStayInTheAppAndShowTheFee() {
        NotificationDraftDto topUp = NotificationComposer.paymentCompleted(payment(PaymentType.TOP_UP, "0.00", null));
        NotificationDraftDto transfer = NotificationComposer.paymentCompleted(
                payment(PaymentType.CARD_TRANSFER, "20.00", null));

        assertThat(topUp.title()).isEqualTo("Поповнення");
        assertThat(topUp.body()).startsWith("+1000.00 UAH");
        assertThat(topUp.emailRequired()).isFalse();
        assertThat(transfer.body()).startsWith("−1000.00 UAH (комісія 20.00 UAH)");
        assertThat(transfer.emailRequired()).isFalse();
    }

    @Test
    void aFailedPaymentIsEmailedAndSaysTheMoneyCameBack() {
        NotificationDraftDto failed = NotificationComposer.paymentFailed(
                payment(PaymentType.BILL_PAYMENT, "5.00", "Rejected by provider"));

        assertThat(failed.emailRequired()).isTrue();
        assertThat(failed.body()).contains("Rejected by provider").contains("1005.00 UAH повернуто");
    }

    @Test
    void creditDecisionsAreAlwaysEmailed() {
        NotificationDraftDto approved = NotificationComposer.creditDecided(decision(CreditApplicationStatus.APPROVED));
        NotificationDraftDto declined = NotificationComposer.creditDecided(decision(CreditApplicationStatus.DECLINED));

        assertThat(approved.emailRequired()).isTrue();
        assertThat(approved.body()).contains("8000.00 UAH");
        assertThat(declined.emailRequired()).isTrue();
    }

    @Test
    void onlyAStatementThatCostsMoneyIsEmailed() {
        assertThat(NotificationComposer.statementSettled(statement(CreditStatementStatus.PAID)).emailRequired())
                .isFalse();
        assertThat(NotificationComposer.statementSettled(statement(CreditStatementStatus.MINIMUM_PAID)).emailRequired())
                .isTrue();

        NotificationDraftDto overdue = NotificationComposer.statementSettled(statement(CreditStatementStatus.OVERDUE));
        assertThat(overdue.emailRequired()).isTrue();
        assertThat(overdue.body()).contains("30.11.2026").contains("71.50 UAH відсотків").contains("100.00 UAH штрафу");
    }

    private static PaymentMessage payment(PaymentType type, String fee, String failureReason) {
        return new PaymentMessage(UUID.randomUUID(), USER, type,
                failureReason == null ? PaymentStatus.COMPLETED : PaymentStatus.FAILED, Currency.UAH,
                new BigDecimal("1000.00"), new BigDecimal(fee), "Опис", failureReason, Instant.now());
    }

    private static CreditDecisionMessage decision(CreditApplicationStatus status) {
        return new CreditDecisionMessage(UUID.randomUUID(), USER, status, DecisionSource.AUTO,
                new BigDecimal("8000.00"), status == CreditApplicationStatus.APPROVED ? new BigDecimal("8000.00") : null,
                Instant.now());
    }

    private static CreditStatementMessage statement(CreditStatementStatus status) {
        return new CreditStatementMessage(UUID.randomUUID(), USER, "2026-10", status, new BigDecimal("3060.00"),
                new BigDecimal("200.00"), new BigDecimal("71.50"),
                status == CreditStatementStatus.OVERDUE ? new BigDecimal("100.00") : BigDecimal.ZERO,
                LocalDate.of(2026, 11, 30), Instant.now());
    }
}
