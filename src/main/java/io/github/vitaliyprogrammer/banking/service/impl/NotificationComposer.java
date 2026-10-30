package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.response.NotificationDraftDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.event.CreditDecisionMessage;
import io.github.vitaliyprogrammer.banking.event.CreditStatementMessage;
import io.github.vitaliyprogrammer.banking.event.PaymentMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Turns an event into words for the client. Pure functions of the message: no database, no clock,
 * so every text and every "is this worth an email" rule is a unit test.
 * Email only when something went wrong or costs money; everyday payments stay in the app.
 */
public final class NotificationComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private NotificationComposer() {
    }

    public static NotificationDraftDto paymentCompleted(PaymentMessage payment) {

        boolean incoming = payment.type() == PaymentType.TOP_UP;
        String amount = (incoming ? "+" : "−") + money(payment.amount(), payment.currency());
        String fee = payment.fee() != null && payment.fee().signum() > 0
                ? " (комісія " + money(payment.fee(), payment.currency()) + ")"
                : "";

        return new NotificationDraftDto(payment.userId(), titleOf(payment.type()),
                amount + fee + ". " + payment.description(), false);
    }

    public static NotificationDraftDto paymentFailed(PaymentMessage payment) {

        return new NotificationDraftDto(payment.userId(), "Платіж не виконано",
                payment.description() + ": " + payment.failureReason() + ". "
                        + money(payment.amount().add(payment.fee()), payment.currency()) + " повернуто на рахунок.",
                true);
    }

    public static NotificationDraftDto creditDecided(CreditDecisionMessage decision) {

        return switch (decision.status()) {
            case APPROVED -> new NotificationDraftDto(decision.userId(), "Кредитний ліміт схвалено",
                    "Ваш ліміт: " + money(decision.approvedLimit(), Currency.UAH)
                            + ". Кредитна картка вже у застосунку. Витрати місяця без відсотків до кінця наступного.",
                    true);
            case DECLINED -> new NotificationDraftDto(decision.userId(), "Заявку на кредит відхилено",
                    "На жаль, зараз ми не можемо надати кредитний ліміт. Причини — у деталях заявки.", true);
            case MANUAL_REVIEW -> throw new IllegalArgumentException("Not a decision: " + decision.status());
        };
    }

    public static NotificationDraftDto statementSettled(CreditStatementMessage statement) {

        String dueDate = statement.dueDate().format(DATE);

        return switch (statement.status()) {
            case PAID -> new NotificationDraftDto(statement.userId(), "Виписку за " + statement.period() + " погашено",
                    "Дякуємо! Борг повернуто вчасно, відсотки не нараховано.", false);
            case MINIMUM_PAID -> new NotificationDraftDto(statement.userId(),
                    "Нараховано відсотки за " + statement.period(),
                    "Виписку не погашено повністю до " + dueDate + ": нараховано "
                            + money(statement.interest(), Currency.UAH) + " відсотків.", true);
            case OVERDUE -> new NotificationDraftDto(statement.userId(),
                    "Прострочено платіж за " + statement.period(),
                    "Мінімальний платіж не внесено до " + dueDate + ". Нараховано "
                            + money(statement.interest(), Currency.UAH) + " відсотків і "
                            + money(statement.lateFee(), Currency.UAH) + " штрафу.", true);
            case OPEN -> throw new IllegalArgumentException("Not settled: " + statement.status());
        };
    }

    private static String titleOf(PaymentType type) {

        return switch (type) {
            case TOP_UP -> "Поповнення";
            case TRANSFER -> "Переказ";
            case CARD_TRANSFER -> "Переказ з картки";
            case BILL_PAYMENT -> "Оплата послуг";
            case EXCHANGE -> "Обмін валют";
            case SAVINGS_DEPOSIT -> "Поповнення скарбнички";
            case SAVINGS_WITHDRAWAL -> "Зняття зі скарбнички";
            case CREDIT_REPAYMENT -> "Погашення кредиту";
        };
    }

    private static String money(BigDecimal amount, Currency currency) {

        return amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString() + " " + currency;
    }
}
