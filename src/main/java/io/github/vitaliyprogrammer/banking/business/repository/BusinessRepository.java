package io.github.vitaliyprogrammer.banking.business.repository;

import io.github.vitaliyprogrammer.banking.business.entity.Business;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByOwnerId(UUID ownerId);

    Optional<Business> findByBusinessAccountId(UUID businessAccountId);

    boolean existsByOwnerIdOrTaxId(UUID ownerId, String taxId);
}
