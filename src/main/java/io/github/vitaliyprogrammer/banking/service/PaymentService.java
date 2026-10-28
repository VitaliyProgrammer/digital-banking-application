package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto topUp(UUID userId, String idempotencyKey, TopUpRequestDto request);

    PaymentResponseDto transfer(UUID userId, String idempotencyKey, TransferRequestDto request);

    PaymentResponseDto cardTransfer(UUID userId, String idempotencyKey, CardTransferRequestDto request);

    PaymentResponseDto getPayment(UUID userId, UUID paymentId);
}
