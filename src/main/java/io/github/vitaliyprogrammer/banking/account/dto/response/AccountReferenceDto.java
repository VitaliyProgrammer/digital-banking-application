package io.github.vitaliyprogrammer.banking.account.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * What other modules need to know about an account to move money: identity, owner and currency.
 * Never sent to clients (it exposes the owner id) and carries no balance, which only the ledger may read under lock.
 */
@NamedInterface("api")
public record AccountReferenceDto(
        UUID id,
        UUID ownerId,
        String number,
        Currency currency
) {

    public static AccountReferenceDto from(Account account) {
        return new AccountReferenceDto(account.getId(), account.getOwnerId(), account.getNumber(), account.getCurrency());
    }
}
