package io.github.vitaliyprogrammer.banking.controller;

import io.github.vitaliyprogrammer.banking.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.dto.response.ReceiptFileDto;
import io.github.vitaliyprogrammer.banking.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/{paymentId}/receipt")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "PDF receipts of completed payments")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * No "produces = application/pdf" on purpose: an error must still come back as JSON problem details,
     * and a client that asked only for PDF would otherwise get 406 instead of the real error.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Download a receipt",
            description = "PDF receipt of one of your completed payments. 409 if the payment is not completed yet")
    public ResponseEntity<byte[]> getReceipt(@CurrentUserId UUID userId,
                                             @PathVariable UUID paymentId) {

        ReceiptFileDto receipt = receiptService.getReceipt(userId, paymentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(receipt.fileName()).build().toString())
                .body(receipt.content());
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Email a receipt",
            description = "Sends the PDF receipt to your email as an attachment")
    public void emailReceipt(@CurrentUserId UUID userId,
                             @PathVariable UUID paymentId) {

        receiptService.emailReceipt(userId, paymentId);
    }
}
