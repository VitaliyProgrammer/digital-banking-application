package io.github.vitaliyprogrammer.banking.dto.response;

import java.util.List;

/**
 * Internal: everything printed on a receipt, already as text (formatted amounts, Kyiv time).
 * The PDF renderer only lays it out; it knows nothing about payments.
 */
public record ReceiptDocumentDto(
        String receiptNumber,
        String title,
        String amount,
        String secondAmount,
        String operation,
        List<Row> rows,
        String transactionId
) {

    /** One "label: value" line. {@code note} is a smaller grey line under the value; {@code total} is bold. */
    public record Row(String label, String value, String note, boolean total) {

        public static Row of(String label, String value) {
            return new Row(label, value, null, false);
        }

        public static Row of(String label, String value, String note) {
            return new Row(label, value, note, false);
        }

        public static Row total(String label, String value) {
            return new Row(label, value, null, true);
        }
    }
}
