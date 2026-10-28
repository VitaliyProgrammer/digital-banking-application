package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.ProviderCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An external company the bank pays on the client's behalf. Reference data: created by migrations, read-only. */
@Entity
@Table(name = "providers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Provider {

    @Id
    private UUID id;

    /** Part of the provider's API path, e.g. "city-power". */
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private ProviderCategory category;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private boolean active;
}
