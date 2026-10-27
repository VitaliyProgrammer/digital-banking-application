package io.github.vitaliyprogrammer.banking.account.repository;

import io.github.vitaliyprogrammer.banking.account.entity.LedgerTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
}
