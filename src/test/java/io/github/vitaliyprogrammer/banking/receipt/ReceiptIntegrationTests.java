package io.github.vitaliyprogrammer.banking.receipt;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.exception.PaymentNotFoundException;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.payment.service.impl.BillPaymentProcessor;
import io.github.vitaliyprogrammer.banking.receipt.dto.response.ReceiptFileDto;
import io.github.vitaliyprogrammer.banking.receipt.exception.ReceiptNotAvailableException;
import io.github.vitaliyprogrammer.banking.receipt.service.ReceiptService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 6. Every kind of completed payment gets a readable PDF with the same number on every
 * download, nobody gets a receipt for someone else's or an unfinished payment, and the email really arrives
 * (Mailpit, a real SMTP server in a container). Rendered receipts are saved to target/receipts for a visual check.
 */
class ReceiptIntegrationTests extends IntegrationTest {

    private static final String RECEIPT_NUMBER = "R-\\d{4}-\\d{8}";

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CardService cardService;

    @Autowired
    private ExchangeQuoteService exchangeQuoteService;

    @Autowired
    private UserService userService;

    @Autowired
    private BillPaymentProcessor billPaymentProcessor;

    private UUID clientId;

    private UUID uahAccount;

    @BeforeEach
    void clientWithMoney() {
        clientId = createUser();
        uahAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(uahAccount, new BigDecimal("10000.00")));
    }

    @Test
    void transferReceiptIsAReadablePdfWithItsNumberAndTransactionId() throws IOException {
        PaymentResponseDto payment = transferToAnotherClient("1250.40");

        ReceiptFileDto receipt = receiptService.getReceipt(clientId, payment.id());
        String text = textOf(receipt, "transfer");

        assertThat(receipt.fileName()).matches(RECEIPT_NUMBER + "\\.pdf");
        assertThat(new String(receipt.content(), 0, 5)).isEqualTo("%PDF-");
        // Cyrillic, the hryvnia sign and the thousands separator survive: the font is really embedded.
        assertThat(text).contains("Переказ на рахунок", "1 250,40 ₴", "ВИКОНАНО",
                receipt.fileName().replace(".pdf", ""), payment.ledgerTransactionId().toString());
    }

    @Test
    void aPaymentKeepsItsReceiptNumberAndAnotherPaymentGetsAnotherOne() {
        PaymentResponseDto first = transferToAnotherClient("10.00");
        PaymentResponseDto second = transferToAnotherClient("20.00");

        String number = receiptService.getReceipt(clientId, first.id()).fileName();

        assertThat(receiptService.getReceipt(clientId, first.id()).fileName()).isEqualTo(number);
        assertThat(receiptService.getReceipt(clientId, second.id()).fileName()).isNotEqualTo(number);
    }

    @Test
    void everyKindOfPaymentGetsItsOwnReceipt() throws IOException {
        wireMock.stubFor(post(urlPathMatching("/.+/payments")).willReturn(okJson("{\"status\": \"ACCEPTED\"}")));
        PaymentResponseDto bill = payBill("city-power", "4410012345", "1250.40");
        PaymentResponseDto mobile = payBill("mobile-one", "+380671234567", "200.00");
        billPaymentProcessor.processPending();

        assertThat(textOf(receiptService.getReceipt(clientId, bill.id()), "utility"))
                .contains("Комунальні послуги: електроенергія", "Особовий рахунок", "4410012345");
        assertThat(textOf(receiptService.getReceipt(clientId, mobile.id()), "mobile"))
                .contains("Поповнення мобільного", "Номер телефону", "+380671234567");
        assertThat(textOf(receiptService.getReceipt(clientId, cardTransfer().id()), "card"))
                .contains("Переказ з картки на картку", "Картка VIRTUAL ••••");
        assertThat(textOf(receiptService.getReceipt(clientId, exchange().id()), "exchange"))
                .contains("Обмін валюти між власними рахунками", "1 $ = 41,0000 ₴", "+24,39 $");
    }

    @Test
    void noReceiptForAnUnfinishedPaymentOrForSomeoneElse() {
        // The provider never answers in this test, so the bill payment stays PROCESSING.
        PaymentResponseDto processing = payBill("city-water", "777", "100.00");
        PaymentResponseDto transfer = transferToAnotherClient("10.00");
        UUID stranger = createUser();

        assertThatThrownBy(() -> receiptService.getReceipt(clientId, processing.id()))
                .isInstanceOf(ReceiptNotAvailableException.class);
        assertThatThrownBy(() -> receiptService.getReceipt(stranger, transfer.id()))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void emailedReceiptArrivesWithThePdfAttached() {
        PaymentResponseDto payment = transferToAnotherClient("50.00");
        String email = userService.getById(clientId).email();

        receiptService.emailReceipt(clientId, payment.id());

        // The mail server is shared by the whole run, so look only at this client's emails.
        String messages = mailServer.messages();
        List<Integer> attachments = JsonPath.read(messages,
                "$.messages[?(@.To[0].Address == '" + email + "')].Attachments");
        List<String> subjects = JsonPath.read(messages, "$.messages[?(@.To[0].Address == '" + email + "')].Subject");

        assertThat(attachments).containsExactly(1);
        assertThat(subjects.getFirst()).matches("Квитанція " + RECEIPT_NUMBER);
    }

    private PaymentResponseDto transferToAnotherClient(String amount) {
        UUID recipient = createUser();
        String recipientIban = accountService.open(recipient, new OpenAccountRequestDto(Currency.UAH)).number();
        return paymentService.transfer(clientId, newKey(),
                new TransferRequestDto(uahAccount, recipientIban, new BigDecimal(amount), null));
    }

    private PaymentResponseDto payBill(String providerCode, String accountNumber, String amount) {
        UUID providerId = jdbcTemplate.queryForObject("SELECT id FROM providers WHERE code = ?", UUID.class,
                providerCode);
        return paymentService.payBill(clientId, newKey(),
                new BillPaymentRequestDto(providerId, accountNumber, uahAccount, new BigDecimal(amount)));
    }

    private PaymentResponseDto cardTransfer() {
        UUID myCard = cardService.issue(clientId, new IssueCardRequestDto(uahAccount, CardType.VIRTUAL)).id();
        UUID recipient = createUser();
        UUID recipientAccount = accountService.open(recipient, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID recipientCard = cardService.issue(recipient,
                new IssueCardRequestDto(recipientAccount, CardType.PAYROLL)).id();
        String recipientNumber = cardService.getCardDetails(recipient, recipientCard).number();
        return paymentService.cardTransfer(clientId, newKey(),
                new CardTransferRequestDto(myCard, recipientNumber, new BigDecimal("500.00"), "За каву і круасани"));
    }

    private PaymentResponseDto exchange() {
        jdbcTemplate.update("""
                INSERT INTO bank_rates (id, currency, buy, sell, mode, official_rate, valid_from)
                VALUES (gen_random_uuid(), 'USD', 40.0000, 41.0000, 'MANUAL', 40.5000, clock_timestamp())
                """);
        UUID usdAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.USD)).id();
        UUID quoteId = exchangeQuoteService.createQuote(clientId,
                new ExchangeQuoteRequestDto(uahAccount, usdAccount, new BigDecimal("1000.00"))).id();
        return paymentService.exchange(clientId, newKey(), new ExchangeRequestDto(quoteId));
    }

    /** Saves the PDF for a look by eye and returns its text, as a PDF reader would extract it. */
    private static String textOf(ReceiptFileDto receipt, String name) throws IOException {
        Path folder = Files.createDirectories(Path.of("target", "receipts"));
        Files.write(folder.resolve(name + ".pdf"), receipt.content());

        try (PDDocument pdf = Loader.loadPDF(receipt.content())) {
            // Amounts use non-breaking spaces; compare them as ordinary ones.
            return new PDFTextStripper().getText(pdf).replace(' ', ' ');
        }
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
