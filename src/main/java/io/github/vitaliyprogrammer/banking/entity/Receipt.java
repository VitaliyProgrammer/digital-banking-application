package io.github.vitaliyprogrammer.banking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The receipt number of a payment. Created once by a native insert (the number comes from a DB sequence). */
@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt {

    @Id
    private UUID paymentId;

    /** E.g. R-2026-00001234: easy to read out to support over the phone. */
    private String number;

    private Instant issuedAt;
}
