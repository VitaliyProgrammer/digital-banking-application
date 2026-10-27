package io.github.vitaliyprogrammer.banking.account.entity.status;

import org.springframework.modulith.NamedInterface;

/** ISO 4217 codes of the currencies the bank works with. */
@NamedInterface("api")
public enum Currency {
    UAH,
    USD,
    EUR
}
