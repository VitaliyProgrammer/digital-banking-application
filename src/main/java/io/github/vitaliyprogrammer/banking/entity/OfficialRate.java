package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The NBU rate of one currency for one day. Written once by the rate sync (native insert), read-only here. */
@Entity
@Table(name = "official_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfficialRate {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private LocalDate rateDate;

    /** UAH per 1 unit of the currency. */
    private BigDecimal rate;

    private Instant fetchedAt;
}
