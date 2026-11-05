package io.github.vitaliyprogrammer.banking.tax.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * What has happened with one tax period of a sole proprietor: how much is paid, and which notices went out.
 * The amount owed is not stored: it is worked out from the tax group and the income, so it can never go stale.
 * The row exists once something happened (a payment or a notice) and is changed only under its row lock.
 */
@Entity
@Table(name = "tax_obligations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxObligation {

    @Id
    private UUID id;

    private UUID businessId;

    private String period;

    private BigDecimal paidAmount;

    private UUID lastPaymentId;

    private Instant reminderSentAt;

    private Instant shortfallNotifiedAt;

    private Instant createdAt;

    public void paid(BigDecimal amount, UUID paymentId) {
        paidAmount = paidAmount.add(amount);
        lastPaymentId = paymentId;
        // Paid now: if it is ever short again (more group 3 income), the client should hear about it again.
        shortfallNotifiedAt = null;
    }

    public void reminded(Instant now) {
        reminderSentAt = now;
    }

    public void shortfallNotified(Instant now) {
        shortfallNotifiedAt = now;
    }
}
