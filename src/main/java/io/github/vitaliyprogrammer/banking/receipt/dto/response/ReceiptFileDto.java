package io.github.vitaliyprogrammer.banking.receipt.dto.response;

/** A rendered receipt: the PDF bytes and a file name like R-2026-00001234.pdf. */
public record ReceiptFileDto(
        String fileName,
        byte[] content
) {
}
