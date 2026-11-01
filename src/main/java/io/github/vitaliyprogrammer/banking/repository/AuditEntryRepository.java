package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.AuditEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Specifications build the admin's search from whichever filters were given. */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID>, JpaSpecificationExecutor<AuditEntry> {
}
