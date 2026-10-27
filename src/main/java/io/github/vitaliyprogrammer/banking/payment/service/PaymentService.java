package io.github.vitaliyprogrammer.banking.payment.service;

import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto topUp(UUID userId, String idempotencyKey, TopUpRequestDto request);

    PaymentResponseDto transfer(UUID userId, String idempotencyKey, TransferRequestDto request);

    PaymentResponseDto getPayment(UUID userId, UUID paymentId);
}
