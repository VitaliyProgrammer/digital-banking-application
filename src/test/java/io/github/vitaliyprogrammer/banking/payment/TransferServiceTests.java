package io.github.vitaliyprogrammer.banking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto.Line;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import io.github.vitaliyprogrammer.banking.business.service.BusinessService;
import io.github.vitaliyprogrammer.banking.card.dto.response.CardReferenceDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.exception.CardNotUsableException;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.outbox.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.outbox.service.OutboxService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.entity.Payment;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.exception.CurrencyMismatchException;
import io.github.vitaliyprogrammer.banking.payment.exception.IdempotencyKeyReusedException;
import io.github.vitaliyprogrammer.banking.payment.exception.RecipientNotFoundException;
import io.github.vitaliyprogrammer.banking.payment.exception.SameAccountTransferException;
import io.github.vitaliyprogrammer.banking.payment.repository.PaymentRepository;
import io.github.vitaliyprogrammer.banking.payment.repository.ProviderRepository;
import io.github.vitaliyprogrammer.banking.payment.service.impl.PaymentMetrics;
import io.github.vitaliyprogrammer.banking.payment.service.impl.PaymentServiceImpl;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.BillPaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.CardTransferFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.CreditRepaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.ExchangeFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.FeeCalculator;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.SavingsDepositFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.SavingsWithdrawalFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TaxPaymentFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TopUpFeePolicy;
import io.github.vitaliyprogrammer.banking.payment.service.impl.fee.TransferFeePolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What {@link PaymentServiceImpl} asks the ledger to do for a transfer, with every other module mocked: who is
 * debited and credited, with which fee, and what is refused before any money moves. That the money really moves
 * in PostgreSQL is proven by the integration tests; here every rule takes milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private final UUID userId = UUID.randomUUID();

    private final AccountReferenceDto from = account(userId, Currency.UAH);

    private final AccountReferenceDto to = account(UUID.randomUUID(), Currency.UAH);

    private final UUID feeAccountId = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private CardService cardService;

    @Mock
    private ExchangeQuoteService exchangeQuoteService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private BusinessService businessService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private TransactionTemplate transactionTemplate;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        FeeCalculator feeCalculator = new FeeCalculator(List.of(new TopUpFeePolicy(), new TransferFeePolicy(),
                new CardTransferFeePolicy(), new BillPaymentFeePolicy(), new ExchangeFeePolicy(),
                new SavingsDepositFeePolicy(), new SavingsWithdrawalFeePolicy(), new CreditRepaymentFeePolicy(),
                new TaxPaymentFeePolicy()));
        paymentService = new PaymentServiceImpl(paymentRepository, providerRepository, accountService, cardService,
                exchangeQuoteService, ledgerService, businessService, feeCalculator, outboxService, paymentMetrics,
                transactionTemplate, Clock.fixed(NOW, ZoneOffset.UTC));

        // lenient: the tests that refuse a payment early never reach these, and that is exactly their point.
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(call -> call.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        lenient().when(paymentRepository.saveAndFlush(any())).thenAnswer(returnsFirstArg());
        lenient().when(paymentRepository.findByInitiatorIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void transferIsOneBalancedPostingWithTheFee() {
        recipientFound();
        when(ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH)).thenReturn(feeAccountId);

        PaymentResponseDto payment = paymentService.transfer(userId, "key-1", transfer("1000.00"));

        PostingRequestDto posting = postedOnce();
        assertThat(posting.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(posting.lines()).containsExactly(
                new Line(from.id(), EntryDirection.DEBIT, new BigDecimal("1005.00")),
                new Line(to.id(), EntryDirection.CREDIT, new BigDecimal("1000.00")),
                new Line(feeAccountId, EntryDirection.CREDIT, new BigDecimal("5.00")));
        assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.fee()).isEqualByComparingTo("5.00");
        // The event is recorded in the same transaction as the money (outbox), so it cannot be lost or invented.
        verify(outboxService).record(eq(OutboxEventType.PAYMENT_COMPLETED), any(), any());
    }

    @Test
    void ibanIsFoundAsTypedWithSpacesAndInLowerCase() {
        when(accountService.getOwnedAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.findClientAccountByNumber(to.number())).thenReturn(Optional.of(to));
        when(ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH)).thenReturn(feeAccountId);

        String typed = to.number().toLowerCase().replaceAll("(.{4})", "$1 ");
        paymentService.transfer(userId, "key-1", new TransferRequestDto(from.id(), typed, new BigDecimal("100.00"), null));

        verify(accountService).findClientAccountByNumber(to.number());
    }

    @Test
    void transferToTheSameAccountIsRefused() {
        when(accountService.getOwnedAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.findClientAccountByNumber(from.number())).thenReturn(Optional.of(from));

        assertThatThrownBy(() -> paymentService.transfer(userId, "key-1",
                new TransferRequestDto(from.id(), from.number(), BigDecimal.TEN, null)))
                .isInstanceOf(SameAccountTransferException.class);

        nothingMoved();
    }

    @Test
    void transferBetweenCurrenciesIsRefused() {
        AccountReferenceDto dollars = account(UUID.randomUUID(), Currency.USD);
        when(accountService.getOwnedAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.findClientAccountByNumber(dollars.number())).thenReturn(Optional.of(dollars));

        // Only an exchange, at the bank's rate, turns hryvnias into dollars.
        assertThatThrownBy(() -> paymentService.transfer(userId, "key-1",
                new TransferRequestDto(from.id(), dollars.number(), BigDecimal.TEN, null)))
                .isInstanceOf(CurrencyMismatchException.class);

        nothingMoved();
    }

    @Test
    void unknownIbanIsRefused() {
        when(accountService.getOwnedAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.findClientAccountByNumber(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.transfer(userId, "key-1", transfer("10.00")))
                .isInstanceOf(RecipientNotFoundException.class);

        nothingMoved();
    }

    @Test
    void sameKeyAndSameRequestReturnTheFirstPayment() {
        recipientFound();
        when(ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH)).thenReturn(feeAccountId);
        PaymentResponseDto first = paymentService.transfer(userId, "key-1", transfer("1000.00"));
        Payment saved = savedPayment();
        when(paymentRepository.findByInitiatorIdAndIdempotencyKey(userId, "key-1")).thenReturn(Optional.of(saved));

        // A retry after a lost answer, with the amount written differently: still the same request.
        PaymentResponseDto retry = paymentService.transfer(userId, "key-1", transfer("1000"));

        assertThat(retry).isEqualTo(first);
        verify(ledgerService, times(1)).post(any());
    }

    @Test
    void sameKeyWithADifferentRequestIsRefused() {
        recipientFound();
        when(ledgerService.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH)).thenReturn(feeAccountId);
        paymentService.transfer(userId, "key-1", transfer("1000.00"));
        Payment saved = savedPayment();
        when(paymentRepository.findByInitiatorIdAndIdempotencyKey(userId, "key-1")).thenReturn(Optional.of(saved));

        // Most likely a client bug: silently returning the old payment would hide that 2000 was never sent.
        assertThatThrownBy(() -> paymentService.transfer(userId, "key-1", transfer("2000.00")))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        verify(ledgerService, times(1)).post(any());
    }

    @Test
    void cardToCardFromAStudentCardIsFreeAndMayUseCredit() {
        CardReferenceDto fromCard = card(userId, from.id(), CardType.STUDENT, true);
        CardReferenceDto toCard = card(to.ownerId(), to.id(), CardType.UNIVERSAL, true);
        when(cardService.getOwnedCardReference(userId, fromCard.id())).thenReturn(fromCard);
        when(cardService.findCardByNumber("4999991234567893")).thenReturn(Optional.of(toCard));
        when(accountService.getOwnedSpendingAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.getAccountReference(to.id())).thenReturn(to);

        paymentService.cardTransfer(userId, "key-1", new CardTransferRequestDto(fromCard.id(), "4999-9912-3456-7893",
                new BigDecimal("300.00"), null));

        PostingRequestDto posting = postedOnce();
        // A card payment: the ledger may take it from the credit limit, unlike a transfer by IBAN.
        assertThat(posting.type()).isEqualTo(TransactionType.CARD_TRANSFER);
        // No fee, so no third line and no fee account at all.
        assertThat(posting.lines()).containsExactly(
                new Line(from.id(), EntryDirection.DEBIT, new BigDecimal("300.00")),
                new Line(to.id(), EntryDirection.CREDIT, new BigDecimal("300.00")));
        verify(ledgerService, never()).getBankAccountId(any(), any());
    }

    @Test
    void blockedCardPaysNothing() {
        CardReferenceDto blocked = card(userId, from.id(), CardType.UNIVERSAL, false);
        when(cardService.getOwnedCardReference(userId, blocked.id())).thenReturn(blocked);

        assertThatThrownBy(() -> paymentService.cardTransfer(userId, "key-1",
                new CardTransferRequestDto(blocked.id(), "4999991234567893", BigDecimal.TEN, null)))
                .isInstanceOf(CardNotUsableException.class)
                .hasMessageContaining("Your card");

        nothingMoved();
    }

    private void recipientFound() {
        when(accountService.getOwnedAccountReference(userId, from.id())).thenReturn(from);
        when(accountService.findClientAccountByNumber(to.number())).thenReturn(Optional.of(to));
    }

    private TransferRequestDto transfer(String amount) {
        return new TransferRequestDto(from.id(), to.number(), new BigDecimal(amount), "Rent");
    }

    private PostingRequestDto postedOnce() {
        ArgumentCaptor<PostingRequestDto> posting = ArgumentCaptor.forClass(PostingRequestDto.class);
        verify(ledgerService).post(posting.capture());
        return posting.getValue();
    }

    private Payment savedPayment() {
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(payment.capture());
        return payment.getValue();
    }

    private void nothingMoved() {
        verify(paymentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(ledgerService, outboxService);
    }

    private static AccountReferenceDto account(UUID ownerId, Currency currency) {
        String number = "UA" + String.format("%027d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
        return new AccountReferenceDto(UUID.randomUUID(), ownerId, number, currency, AccountType.CLIENT);
    }

    private static CardReferenceDto card(UUID ownerId, UUID accountId, CardType type, boolean usable) {
        return new CardReferenceDto(UUID.randomUUID(), ownerId, accountId, type, "7893", usable);
    }
}
