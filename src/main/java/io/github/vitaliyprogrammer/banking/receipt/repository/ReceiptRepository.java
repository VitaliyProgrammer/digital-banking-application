package io.github.vitaliyprogrammer.banking.receipt.repository;

import io.github.vitaliyprogrammer.banking.receipt.entity.Receipt;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /**
     * Gives the payment its number unless it already has one, so downloading a receipt twice shows the same
     * number, and two parallel first downloads cannot create two numbers. The sequence value is taken
     * even when the row exists; that only leaves a gap in the numbering.
     */
    @Modifying
    @Query(value = """
            INSERT INTO receipts (payment_id, number, issued_at)
            VALUES (:paymentId, 'R-' || :year || '-' || lpad(nextval('receipt_number_seq')::text, 8, '0'), :issuedAt)
            ON CONFLICT (payment_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID paymentId, int year, Instant issuedAt);
}
