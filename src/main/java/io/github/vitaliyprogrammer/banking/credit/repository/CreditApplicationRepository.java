package io.github.vitaliyprogrammer.banking.credit.repository;

import io.github.vitaliyprogrammer.banking.credit.entity.CreditApplication;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CreditApplicationRepository extends JpaRepository<CreditApplication, UUID> {

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<CreditApplicationStatus> statuses);

    List<CreditApplication> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<CreditApplication> findAllByStatusOrderByCreatedAt(CreditApplicationStatus status, Pageable pageable);

    /** Locked while a reviewer decides: two reviewers clicking at once cannot both decide. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditApplication a where a.id = :id")
    Optional<CreditApplication> findByIdForUpdate(UUID id);
}
