package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.FeeRequest;
import io.github.vitaliyprogrammer.banking.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.ExchangeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderResponseDto;
import io.github.vitaliyprogrammer.banking.entity.Payment;
import io.github.vitaliyprogrammer.banking.entity.Provider;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.exception.CardNotUsableException;
import io.github.vitaliyprogrammer.banking.exception.CurrencyMismatchException;
import io.github.vitaliyprogrammer.banking.exception.ExchangeQuoteAlreadyUsedException;
import io.github.vitaliyprogrammer.banking.exception.ExchangeQuoteExpiredException;
import io.github.vitaliyprogrammer.banking.exception.IdempotencyKeyReusedException;
import io.github.vitaliyprogrammer.banking.exception.PaymentNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.ProviderNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.RecipientNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.SameAccountTransferException;
import io.github.vitaliyprogrammer.banking.repository.PaymentRepository;
import io.github.vitaliyprogrammer.banking.repository.ProviderRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.service.LedgerService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final ProviderRepository providerRepository;

    private final AccountService accountService;

    private final CardService cardService;

    private final ExchangeQuoteService exchangeQuoteService;

    private final LedgerService ledgerService;

    private final FeeCalculator feeCalculator;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    @Override
    public PaymentResponseDto topUp(UUID userId, String idempotencyKey, TopUpRequestDto request) {

        String requestHash = hash(PaymentType.TOP_UP, request.accountId(), request.amount());

        return executeOnce(userId, idempotencyKey, requestHash,
                () -> createTopUp(userId, idempotencyKey, requestHash, request));
    }

    @Override
    public PaymentResponseDto transfer(UUID userId, String idempotencyKey, TransferRequestDto request) {

        String recipientNumber = normalizeAccountNumber(request.toAccountNumber());
        String requestHash = hash(PaymentType.TRANSFER, request.fromAccountId(), recipientNumber,
                request.amount(), request.description());

        return executeOnce(userId, idempotencyKey, requestHash,
                () -> createTransfer(userId, idempotencyKey, requestHash, recipientNumber, request));
    }

    @Override
    public PaymentResponseDto cardTransfer(UUID userId, String idempotencyKey, CardTransferRequestDto request) {

        String recipientCardNumber = normalizeCardNumber(request.toCardNumber());
        String requestHash = hash(PaymentType.CARD_TRANSFER, request.fromCardId(), recipientCardNumber,
                request.amount(), request.description());

        return executeOnce(userId, idempotencyKey, requestHash,
                () -> createCardTransfer(userId, idempotencyKey, requestHash, recipientCardNumber, request));
    }

    @Override
    public PaymentResponseDto payBill(UUID userId, String idempotencyKey, BillPaymentRequestDto request) {

        String accountNumber = request.accountNumber().strip();
        String requestHash = hash(PaymentType.BILL_PAYMENT, request.providerId(), accountNumber,
                request.fromAccountId(), request.amount());

        return executeOnce(userId, idempotencyKey, requestHash,
                () -> createBillPayment(userId, idempotencyKey, requestHash, accountNumber, request));
    }

    @Override
    public PaymentResponseDto exchange(UUID userId, String idempotencyKey, ExchangeRequestDto request) {

        String requestHash = hash(PaymentType.EXCHANGE, request.quoteId());

        return executeOnce(userId, idempotencyKey, requestHash,
                () -> createExchange(userId, idempotencyKey, requestHash, request.quoteId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderResponseDto> getProviders() {

        return providerRepository.findAllByActiveTrueOrderByCategoryAscNameAsc().stream()
                .map(ProviderResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderResponseDto getProvider(UUID providerId) {

        return providerRepository.findById(providerId)
                .map(ProviderResponseDto::from)
                .orElseThrow(ProviderNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(UUID userId, UUID paymentId) {

        return paymentRepository.findByIdAndInitiatorId(paymentId, userId)
                .map(PaymentResponseDto::from)
                .orElseThrow(PaymentNotFoundException::new);
    }

    /**
     * Idempotency in three cases:
     * 1. The key was used before → return that payment (or 422 if the request differs). No money moves.
     * 2. The key is new → run the operation in a fresh transaction. Its first write is the payment row,
     *    so a parallel request with the same key blocks on the unique index right there, before any money moves.
     * 3. That parallel request lost the race → its transaction is rolled back entirely; outside of it,
     *    read the winner's payment and return it, exactly as in case 1.
     * A programmatic transaction is needed because case 3 must read the winner after the rollback, outside of it.
     */
    private PaymentResponseDto executeOnce(UUID userId,
                                           String idempotencyKey,
                                           String requestHash,
                                           Supplier<Payment> operation) {

        Optional<Payment> existing = paymentRepository.findByInitiatorIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        try {
            Payment payment = Objects.requireNonNull(transactionTemplate.execute(status -> operation.get()));
            return PaymentResponseDto.from(payment);
        } catch (DataIntegrityViolationException e) {
            Payment winner = paymentRepository.findByInitiatorIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> e);
            return replay(winner, requestHash);
        }
    }

    private static PaymentResponseDto replay(Payment payment, String requestHash) {

        if (!payment.getRequestHash().equals(requestHash)) {
            throw new IdempotencyKeyReusedException();
        }

        return PaymentResponseDto.from(payment);
    }

    private Payment createTopUp(UUID userId, String idempotencyKey, String requestHash, TopUpRequestDto request) {

        AccountReferenceDto account = accountService.getOwnedAccountReference(userId, request.accountId());
        BigDecimal fee = feeCalculator.calculate(PaymentType.TOP_UP,
                FeeRequest.withoutCard(request.amount(), account.currency()));

        Payment payment = paymentRepository.saveAndFlush(Payment.pending(
                userId, idempotencyKey, requestHash, PaymentType.TOP_UP,
                null, account.id(), account.currency(), request.amount(), fee, "Поповнення рахунку", clock.instant()));

        UUID ledgerTransactionId = ledgerService.post(PostingRequestDto.transfer(
                TransactionType.DEPOSIT, "Поповнення рахунку",
                ledgerService.getBankAccountId(AccountType.SETTLEMENT, account.currency()),
                account.id(),
                request.amount()));

        payment.complete(ledgerTransactionId, clock.instant());

        return payment;
    }

    private Payment createTransfer(UUID userId,
                                   String idempotencyKey,
                                   String requestHash,
                                   String recipientNumber,
                                   TransferRequestDto request) {

        AccountReferenceDto from = accountService.getOwnedAccountReference(userId, request.fromAccountId());
        AccountReferenceDto to = accountService.findClientAccountByNumber(recipientNumber)
                .orElseThrow(RecipientNotFoundException::new);

        requireTransferable(from, to);

        BigDecimal fee = feeCalculator.calculate(PaymentType.TRANSFER,
                FeeRequest.withoutCard(request.amount(), from.currency()));
        String description = request.description() != null ? request.description() : "Переказ на рахунок " + to.number();

        Payment payment = paymentRepository.saveAndFlush(Payment.pending(
                userId, idempotencyKey, requestHash, PaymentType.TRANSFER,
                from.id(), to.id(), from.currency(), request.amount(), fee, description, clock.instant()));

        return moveMoney(payment, from, to);
    }

    /** Same money movement as a transfer; the difference is how both sides are found and what the fee depends on. */
    private Payment createCardTransfer(UUID userId,
                                       String idempotencyKey,
                                       String requestHash,
                                       String recipientCardNumber,
                                       CardTransferRequestDto request) {

        CardReferenceDto fromCard = cardService.getOwnedCardReference(userId, request.fromCardId());
        if (!fromCard.usable()) {
            throw new CardNotUsableException("Your card");
        }

        CardReferenceDto toCard = cardService.findCardByNumber(recipientCardNumber)
                .orElseThrow(RecipientNotFoundException::new);
        if (!toCard.usable()) {
            throw new CardNotUsableException("Recipient's card");
        }

        AccountReferenceDto from = accountService.getOwnedAccountReference(userId, fromCard.accountId());
        AccountReferenceDto to = accountService.getAccountReference(toCard.accountId());

        requireTransferable(from, to);

        BigDecimal fee = feeCalculator.calculate(PaymentType.CARD_TRANSFER,
                new FeeRequest(request.amount(), from.currency(), fromCard.type()));
        String description = request.description() != null
                ? request.description()
                : "Переказ на картку *" + toCard.lastFour();

        Payment payment = Payment.pending(
                userId, idempotencyKey, requestHash, PaymentType.CARD_TRANSFER,
                from.id(), to.id(), from.currency(), request.amount(), fee, description, clock.instant());
        payment.attachCards(fromCard.id(), toCard.id());

        return moveMoney(paymentRepository.saveAndFlush(payment), from, to);
    }

    /**
     * Only the hold happens here, in the request's transaction: the money leaves the client's balance at once
     * (it cannot be spent twice), but nothing has left the bank yet. The provider is called later by
     * {@link BillPaymentProcessor}, outside of any database transaction.
     */
    private Payment createBillPayment(UUID userId,
                                      String idempotencyKey,
                                      String requestHash,
                                      String accountNumber,
                                      BillPaymentRequestDto request) {

        Provider provider = providerRepository.findById(request.providerId())
                .filter(Provider::isActive)
                .orElseThrow(ProviderNotFoundException::new);

        AccountReferenceDto from = accountService.getOwnedAccountReference(userId, request.fromAccountId());

        if (from.currency() != provider.getCurrency()) {
            throw new CurrencyMismatchException();
        }

        BigDecimal fee = feeCalculator.calculate(PaymentType.BILL_PAYMENT,
                FeeRequest.withoutCard(request.amount(), from.currency()));
        String description = provider.getName() + ", " + accountNumber;

        Payment payment = paymentRepository.saveAndFlush(Payment.pendingBill(
                userId, idempotencyKey, requestHash, from.id(), from.currency(), request.amount(), fee,
                provider.getId(), accountNumber, description, clock.instant()));

        UUID holdTransactionId = ledgerService.post(PostingRequestDto.transfer(
                TransactionType.HOLD, description,
                from.id(),
                ledgerService.getBankAccountId(AccountType.HOLD, from.currency()),
                request.amount().add(fee)));

        payment.startProcessing(holdTransactionId, clock.instant());

        return payment;
    }

    /**
     * The quote row stays locked until this transaction ends. A second request with the same quote but another
     * idempotency key waits on that lock, then sees the payment and gets 409. The unique index on
     * payments.quote_id is only the last line of defence. The checks order matters: "used" before "expired",
     * so a client who already exchanged is told exactly that.
     */
    private Payment createExchange(UUID userId, String idempotencyKey, String requestHash, UUID quoteId) {

        ExchangeQuoteResponseDto quote = exchangeQuoteService.lockForUse(userId, quoteId);

        if (paymentRepository.existsByQuoteId(quote.id())) {
            throw new ExchangeQuoteAlreadyUsedException();
        }

        if (!clock.instant().isBefore(quote.expiresAt())) {
            throw new ExchangeQuoteExpiredException();
        }

        BigDecimal fee = feeCalculator.calculate(PaymentType.EXCHANGE,
                FeeRequest.withoutCard(quote.sellAmount(), quote.fromCurrency()));
        String description = "Обмін " + quote.fromCurrency() + " на " + quote.toCurrency();

        Payment payment = paymentRepository.saveAndFlush(Payment.pendingExchange(
                userId, idempotencyKey, requestHash, quote.fromAccountId(), quote.toAccountId(),
                quote.fromCurrency(), quote.sellAmount(), fee, quote.id(), quote.buyAmount(), quote.rate(),
                description, clock.instant()));

        // Four lines, balanced per currency: the client's money goes to the bank's FX position in one currency,
        // and comes out of the FX position in the other.
        UUID ledgerTransactionId = ledgerService.post(new PostingRequestDto(TransactionType.EXCHANGE, description,
                List.of(
                        new PostingRequestDto.Line(quote.fromAccountId(), EntryDirection.DEBIT, quote.sellAmount()),
                        new PostingRequestDto.Line(fxPosition(quote.fromCurrency()), EntryDirection.CREDIT,
                                quote.sellAmount()),
                        new PostingRequestDto.Line(fxPosition(quote.toCurrency()), EntryDirection.DEBIT,
                                quote.buyAmount()),
                        new PostingRequestDto.Line(quote.toAccountId(), EntryDirection.CREDIT, quote.buyAmount()))));

        payment.complete(ledgerTransactionId, clock.instant());

        return payment;
    }

    private UUID fxPosition(Currency currency) {

        return ledgerService.getBankAccountId(AccountType.FX_POSITION, currency);
    }

    private static void requireTransferable(AccountReferenceDto from, AccountReferenceDto to) {

        if (from.id().equals(to.id())) {
            throw new SameAccountTransferException();
        }

        if (from.currency() != to.currency()) {
            throw new CurrencyMismatchException();
        }
    }

    /**
     * The payer pays amount + fee, the recipient gets the amount, the bank's fee account gets the fee:
     * three lines of one balanced ledger transaction, so they succeed or fail together.
     */
    private Payment moveMoney(Payment payment, AccountReferenceDto from, AccountReferenceDto to) {

        BigDecimal amount = payment.getAmount();
        BigDecimal fee = payment.getFee();

        List<PostingRequestDto.Line> lines = new ArrayList<>();
        lines.add(new PostingRequestDto.Line(from.id(), EntryDirection.DEBIT, amount.add(fee)));
        lines.add(new PostingRequestDto.Line(to.id(), EntryDirection.CREDIT, amount));

        if (fee.signum() > 0) {
            UUID feeAccountId = ledgerService.getBankAccountId(AccountType.FEE_INCOME, from.currency());
            lines.add(new PostingRequestDto.Line(feeAccountId, EntryDirection.CREDIT, fee));
        }

        UUID ledgerTransactionId = ledgerService.post(
                new PostingRequestDto(TransactionType.TRANSFER, payment.getDescription(), lines));

        payment.complete(ledgerTransactionId, clock.instant());

        return payment;
    }

    /** "UA04 3009 9977..." copied from an app and "ua043009..." must find the same account. */
    private static String normalizeAccountNumber(String number) {

        return number.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /** "4999 9912 3456 7890" and "4999-9912-3456-7890" are the same card. */
    private static String normalizeCardNumber(String number) {

        return number.replaceAll("[\\s-]", "");
    }

    /**
     * Fingerprint of the request, stored with the payment. 10 and 10.00 are the same amount,
     * so amounts are normalized before hashing.
     */
    private static String hash(Object... parts) {

        StringBuilder canonical = new StringBuilder();

        for (Object part : parts) {
            Object value = part instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : part;
            canonical.append(value).append('|');
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
