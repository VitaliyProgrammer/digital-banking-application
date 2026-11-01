package io.github.vitaliyprogrammer.banking.savings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Interest earned by one savings account for one day. Written by a native insert, read-only here. */
@Entity
@Table(name = "interest_accruals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestAccrual {

    @Id
    private UUID id;

    private UUID accountId;

    private LocalDate accrualDate;

    /** The balance at the end of that day (Kyiv time) the interest was calculated on. */
    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(precision = 5, scale = 2)
    private BigDecimal annualRate;

    @Column(precision = 19, scale = 8)
    private BigDecimal amount;

    private Instant createdAt;
}
