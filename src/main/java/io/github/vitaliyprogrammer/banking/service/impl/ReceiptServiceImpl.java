package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.ReceiptProperties;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ReceiptDocumentDto;
import io.github.vitaliyprogrammer.banking.dto.response.ReceiptDocumentDto.Row;
import io.github.vitaliyprogrammer.banking.dto.response.ReceiptFileDto;
import io.github.vitaliyprogrammer.banking.dto.response.UserResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.entity.status.ProviderCategory;
import io.github.vitaliyprogrammer.banking.exception.ReceiptEmailFailedException;
import io.github.vitaliyprogrammer.banking.exception.ReceiptNotAvailableException;
import io.github.vitaliyprogrammer.banking.repository.ReceiptRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import io.github.vitaliyprogrammer.banking.service.ReceiptService;
import io.github.vitaliyprogrammer.banking.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns a payment into the text of its receipt: Ukrainian labels, "1 250,40 ₴" amounts, Kyiv time.
 * Layout and drawing are the job of {@link ReceiptPdfRenderer}; this class decides what is printed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    /** Times are stored in UTC; a client in Ukraine expects to see the time on their own clock. */
    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");

    /** A real minus sign, not a hyphen: "−505,00 ₴". */
    private static final String MINUS = "−";

    private static final String KYIV_TIME = "за Києвом";

    private final ReceiptRepository receiptRepository;

    private final PaymentService paymentService;

    private final AccountService accountService;

    private final CardService cardService;

    private final UserService userService;

    private final ReceiptPdfRenderer receiptPdfRenderer;

    private final JavaMailSender mailSender;

    private final ReceiptProperties properties;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Override
    public ReceiptFileDto getReceipt(UUID userId, UUID paymentId) {

        PaymentResponseDto payment = paymentService.getPayment(userId, paymentId);

        if (payment.status() != PaymentStatus.COMPLETED) {
            throw new ReceiptNotAvailableException();
        }

        ReceiptDocumentDto document = describe(userService.getById(userId), payment, issueNumber(payment));

        return new ReceiptFileDto(document.receiptNumber() + ".pdf", receiptPdfRenderer.render(document));
    }

    /** Sent synchronously: the client sees at once whether it worked. No transaction is open while sending. */
    @Override
    public void emailReceipt(UUID userId, UUID paymentId) {

        ReceiptFileDto receipt = getReceipt(userId, paymentId);
        String receiptNumber = receipt.fileName().replace(".pdf", "");
        String email = userService.getById(userId).email();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.mailFrom());
            helper.setTo(email);
            helper.setSubject("Квитанція " + receiptNumber);
            helper.setText("Добрий день!\n\nУ вкладенні квитанція " + receiptNumber
                    + " про вашу операцію в Digital Bank.\n\nЦей лист надіслано автоматично, відповідати на нього не потрібно.");
            helper.addAttachment(receipt.fileName(), new ByteArrayResource(receipt.content()),
                    MediaType.APPLICATION_PDF_VALUE);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.warn("Receipt {} was not sent: {}", receiptNumber, e.getMessage());
            throw new ReceiptEmailFailedException();
        }
    }

    /** The year in the number is the year of the operation (in Kyiv), not of the download. */
    private String issueNumber(PaymentResponseDto payment) {

        int year = payment.completedAt().atZone(KYIV).getYear();

        return Objects.requireNonNull(transactionTemplate.execute(_ -> {
            receiptRepository.insertIfAbsent(payment.id(), year, clock.instant());
            return receiptRepository.findById(payment.id()).orElseThrow().getNumber();
        }));
    }

    private ReceiptDocumentDto describe(UserResponseDto payer, PaymentResponseDto payment, String receiptNumber) {

        return switch (payment.type()) {
            case TOP_UP -> topUp(payer, payment, receiptNumber);
            case TRANSFER -> transfer(payer, payment, receiptNumber);
            case CARD_TRANSFER -> cardTransfer(payer, payment, receiptNumber);
            case BILL_PAYMENT -> billPayment(payment, receiptNumber);
            case EXCHANGE -> exchange(payment, receiptNumber);
            case SAVINGS_DEPOSIT, SAVINGS_WITHDRAWAL -> ownTransfer(payment, receiptNumber);
        };
    }

    /** Money moved between two accounts of the same client: no recipient, no sign on the amount. */
    private ReceiptDocumentDto ownTransfer(PaymentResponseDto payment, String receiptNumber) {

        AccountReferenceDto from = accountService.getAccountReference(payment.fromAccountId());
        AccountReferenceDto to = accountService.getAccountReference(payment.toAccountId());
        boolean deposit = payment.type() == PaymentType.SAVINGS_DEPOSIT;

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Списано з", iban(from.number()), deposit ? "поточний рахунок" : "скарбничка"));
        rows.add(Row.of("Зараховано на", iban(to.number()), deposit ? "скарбничка" : "поточний рахунок"));
        rows.add(Row.of("Сума", money(payment.amount(), payment.currency())));
        rows.add(Row.of("Комісія", money(payment.fee(), payment.currency())));
        rows.add(dateRow(payment));

        return document(receiptNumber, "Квитанція про переказ", money(payment.amount(), payment.currency()), null,
                payment.description(), rows, payment);
    }

    private ReceiptDocumentDto topUp(UserResponseDto payer, PaymentResponseDto payment, String receiptNumber) {

        AccountReferenceDto to = accountService.getAccountReference(payment.toAccountId());

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Зараховано на", iban(to.number()), fullName(payer)));
        rows.add(Row.of("Сума", money(payment.amount(), payment.currency())));
        rows.add(Row.of("Комісія", money(payment.fee(), payment.currency())));
        rows.add(dateRow(payment));

        return document(receiptNumber, "Квитанція про поповнення", "+" + money(payment.amount(), payment.currency()),
                null, "Поповнення рахунку", rows, payment);
    }

    private ReceiptDocumentDto transfer(UserResponseDto payer, PaymentResponseDto payment, String receiptNumber) {

        AccountReferenceDto from = accountService.getAccountReference(payment.fromAccountId());
        AccountReferenceDto to = accountService.getAccountReference(payment.toAccountId());

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Відправник", iban(from.number()), fullName(payer)));
        rows.add(Row.of("Отримувач", iban(to.number()), shortName(userService.getById(to.ownerId()))));
        rows.addAll(transferAmounts(payment, "Сума переказу"));

        return document(receiptNumber, "Квитанція про переказ", debited(payment), null,
                "Переказ на рахунок", rows, payment);
    }

    private ReceiptDocumentDto cardTransfer(UserResponseDto payer, PaymentResponseDto payment, String receiptNumber) {

        CardReferenceDto fromCard = cardService.getCardReference(payment.fromCardId());
        CardReferenceDto toCard = cardService.getCardReference(payment.toCardId());

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Відправник", "Картка " + fromCard.type() + " •••• " + fromCard.lastFour(), fullName(payer)));
        rows.add(Row.of("Отримувач", "Картка •••• " + toCard.lastFour(),
                shortName(userService.getById(toCard.ownerId()))));
        rows.addAll(transferAmounts(payment, "Сума переказу"));

        return document(receiptNumber, "Квитанція про переказ", debited(payment), null,
                "Переказ з картки на картку", rows, payment);
    }

    /** A mobile top-up is a bill payment too, but people expect a phone number on it, not "personal account". */
    private ReceiptDocumentDto billPayment(PaymentResponseDto payment, String receiptNumber) {

        ProviderResponseDto provider = paymentService.getProvider(payment.providerId());
        AccountReferenceDto from = accountService.getAccountReference(payment.fromAccountId());
        boolean mobile = provider.category() == ProviderCategory.MOBILE;

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of(mobile ? "Оператор" : "Отримувач", provider.name()));
        rows.add(Row.of(mobile ? "Номер телефону" : "Особовий рахунок", payment.providerAccountNumber()));
        rows.add(Row.of("Рахунок списання", iban(from.number())));
        rows.add(Row.of("Сума", money(payment.amount(), payment.currency())));
        rows.add(Row.of("Комісія", money(payment.fee(), payment.currency())));
        rows.add(Row.total("Разом списано", money(payment.amount().add(payment.fee()), payment.currency())));
        rows.add(dateRow(payment));

        return document(receiptNumber,
                mobile ? "Квитанція про поповнення" : "Квитанція про оплату",
                debited(payment), null, operation(provider.category()), rows, payment);
    }

    private ReceiptDocumentDto exchange(PaymentResponseDto payment, String receiptNumber) {

        AccountReferenceDto from = accountService.getAccountReference(payment.fromAccountId());
        AccountReferenceDto to = accountService.getAccountReference(payment.toAccountId());

        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("Списано з", iban(from.number()), accountKind(from.currency())));
        rows.add(Row.of("Зараховано на", iban(to.number()), accountKind(to.currency())));
        rows.add(Row.of("Курс банку", rate(from.currency(), to.currency(), payment.exchangeRate())));
        rows.add(Row.of("Комісія", money(payment.fee(), payment.currency())));
        rows.add(Row.total("Разом списано", money(payment.amount(), payment.currency())));
        rows.add(dateRow(payment));

        return document(receiptNumber, "Квитанція про обмін", "+" + money(payment.creditedAmount(), to.currency()),
                MINUS + money(payment.amount(), payment.currency()),
                "Обмін валюти між власними рахунками", rows, payment);
    }

    private static List<Row> transferAmounts(PaymentResponseDto payment, String amountLabel) {

        List<Row> rows = new ArrayList<>();
        if (payment.description() != null) {
            rows.add(Row.of("Призначення", payment.description()));
        }
        rows.add(Row.of(amountLabel, money(payment.amount(), payment.currency())));
        rows.add(Row.of("Комісія", money(payment.fee(), payment.currency())));
        rows.add(Row.total("Разом списано", money(payment.amount().add(payment.fee()), payment.currency())));
        rows.add(dateRow(payment));

        return rows;
    }

    private static ReceiptDocumentDto document(String receiptNumber, String title, String amount, String secondAmount,
                                               String operation, List<Row> rows, PaymentResponseDto payment) {

        return new ReceiptDocumentDto(receiptNumber, title, amount, secondAmount, operation, rows,
                payment.ledgerTransactionId().toString());
    }

    private static Row dateRow(PaymentResponseDto payment) {

        return Row.of("Дата і час", DATE_TIME.format(payment.completedAt().atZone(KYIV)), KYIV_TIME);
    }

    private static String debited(PaymentResponseDto payment) {

        return MINUS + money(payment.amount().add(payment.fee()), payment.currency());
    }

    /** "1 250,40 ₴": a non-breaking space between thousands (never split across lines) and a decimal comma. */
    private static String money(BigDecimal amount, Currency currency) {

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);

        return format.format(amount) + " " + symbol(currency);
    }

    /**
     * The rate the way people read it: always "1 unit of the foreign currency = X". For UAH → USD the stored rate
     * is "USD per 1 UAH" (0.0244), so it is turned around to "1 $ = 41,0000 ₴".
     */
    private static String rate(Currency from, Currency to, BigDecimal rate) {

        if (from == Currency.UAH) {
            BigDecimal perUnit = BigDecimal.ONE.divide(rate, 4, RoundingMode.HALF_UP);
            return "1 " + symbol(to) + " = " + number(perUnit) + " " + symbol(from);
        }

        return "1 " + symbol(from) + " = " + number(rate.setScale(4, RoundingMode.HALF_UP)) + " " + symbol(to);
    }

    private static String number(BigDecimal value) {

        return value.toPlainString().replace('.', ',');
    }

    private static String symbol(Currency currency) {

        return switch (currency) {
            case UAH -> "₴";
            case USD -> "$";
            case EUR -> "€";
        };
    }

    private static String accountKind(Currency currency) {

        return switch (currency) {
            case UAH -> "гривневий рахунок";
            case USD -> "доларовий рахунок";
            case EUR -> "євровий рахунок";
        };
    }

    private static String operation(ProviderCategory category) {

        return switch (category) {
            case ELECTRICITY -> "Комунальні послуги: електроенергія";
            case WATER -> "Комунальні послуги: водопостачання";
            case GAS -> "Комунальні послуги: газопостачання";
            case HEATING -> "Комунальні послуги: опалення";
            case INTERNET -> "Оплата інтернету";
            case MOBILE -> "Поповнення мобільного";
        };
    }

    /** "UA04 3009 9977 ...": groups of four, as printed on bank statements. */
    private static String iban(String number) {

        return number.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private static String fullName(UserResponseDto user) {

        return user.firstName() + " " + user.lastName();
    }

    /** "Леся У.": enough to recognise the recipient, without printing a stranger's full name on someone's receipt. */
    private static String shortName(UserResponseDto user) {

        return user.firstName() + " " + user.lastName().charAt(0) + ".";
    }
}
