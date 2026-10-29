package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ProviderResponseDto;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto topUp(UUID userId, String idempotencyKey, TopUpRequestDto request);

    PaymentResponseDto transfer(UUID userId, String idempotencyKey, TransferRequestDto request);

    PaymentResponseDto cardTransfer(UUID userId, String idempotencyKey, CardTransferRequestDto request);

    /** Holds the money and returns at once with status PROCESSING; the provider is called in the background. */
    PaymentResponseDto payBill(UUID userId, String idempotencyKey, BillPaymentRequestDto request);

    /** Spends a quote: exactly the amounts and the rate the client saw, or an error if it expired or was used. */
    PaymentResponseDto exchange(UUID userId, String idempotencyKey, ExchangeRequestDto request);

    List<ProviderResponseDto> getProviders();

    PaymentResponseDto getPayment(UUID userId, UUID paymentId);
}
