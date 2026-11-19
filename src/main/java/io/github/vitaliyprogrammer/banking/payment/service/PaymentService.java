package io.github.vitaliyprogrammer.banking.payment.service;

import io.github.vitaliyprogrammer.banking.payment.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CreditRepaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.FeeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.ProviderResponseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto topUp(UUID userId, String idempotencyKey, TopUpRequestDto request);

    PaymentResponseDto transfer(UUID userId, String idempotencyKey, TransferRequestDto request);

    PaymentResponseDto cardTransfer(UUID userId, String idempotencyKey, CardTransferRequestDto request);

    /** The fee of a transfer by IBAN from one of the client's accounts, before it is sent. Moves no money. */
    FeeQuoteResponseDto quoteTransferFee(UUID userId, UUID fromAccountId, BigDecimal amount);

    /** The fee of a card-to-card transfer from one of the client's cards: it depends on the card's type. */
    FeeQuoteResponseDto quoteCardTransferFee(UUID userId, UUID fromCardId, BigDecimal amount);

    /** Holds the money and returns at once with status PROCESSING; the provider is called in the background. */
    PaymentResponseDto payBill(UUID userId, String idempotencyKey, BillPaymentRequestDto request);

    /** Spends a quote: exactly the amounts and the rate the client saw, or an error if it expired or was used. */
    PaymentResponseDto exchange(UUID userId, String idempotencyKey, ExchangeRequestDto request);

    /** Current account → the same client's savings jar. */
    PaymentResponseDto depositToSavings(UUID userId, String idempotencyKey, UUID savingsId,
                                        SavingsTransferRequestDto request);

    /** Savings jar → the same client's current account. The only way money leaves a jar. */
    PaymentResponseDto withdrawFromSavings(UUID userId, String idempotencyKey, UUID savingsId,
                                           SavingsTransferRequestDto request);

    /**
     * For the tax module: the single tax from a sole proprietor's tax jar or business account to the tax office.
     * Idempotent like every payment: the same key never pays twice.
     */
    PaymentResponseDto payTax(UUID userId, String idempotencyKey, UUID fromAccountId, BigDecimal amount,
                              String description);

    /** Current UAH account → the same client's credit line. Never more than the debt. */
    PaymentResponseDto repayCredit(UUID userId, String idempotencyKey, CreditRepaymentRequestDto request);

    List<ProviderResponseDto> getProviders();

    /** Any provider by id, also an inactive one: an old payment still shows who was paid. */
    ProviderResponseDto getProvider(UUID providerId);

    PaymentResponseDto getPayment(UUID userId, UUID paymentId);

    /** Where the client's money went between two days, both included, in Kyiv time. */
    SpendingResponseDto getSpending(UUID userId, LocalDate from, LocalDate to);
}
