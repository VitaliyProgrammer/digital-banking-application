package io.github.vitaliyprogrammer.banking.receipt.service;

import io.github.vitaliyprogrammer.banking.receipt.dto.response.ReceiptFileDto;
import java.util.UUID;

public interface ReceiptService {

    /** PDF receipt of a completed payment of this user. Someone else's payment is 404. */
    ReceiptFileDto getReceipt(UUID userId, UUID paymentId);

    /** Sends the same PDF to the user's email as an attachment. */
    void emailReceipt(UUID userId, UUID paymentId);
}
