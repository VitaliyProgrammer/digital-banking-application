package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.LedgerTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
}
