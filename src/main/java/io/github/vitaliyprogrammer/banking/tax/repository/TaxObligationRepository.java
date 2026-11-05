package io.github.vitaliyprogrammer.banking.tax.repository;

import io.github.vitaliyprogrammer.banking.tax.entity.TaxObligation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TaxObligationRepository extends JpaRepository<TaxObligation, UUID> {

    List<TaxObligation> findAllByBusinessId(UUID businessId);

    /**
     * Makes sure the row exists without failing the transaction when it already does: a plain insert would hit the
     * unique index and, in PostgreSQL, abort the whole transaction, so the payment around it could not go on.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tax_obligations (id, business_id, period, paid_amount, created_at)
            VALUES (gen_random_uuid(), :businessId, :period, 0, now())
            ON CONFLICT (business_id, period) DO NOTHING""", nativeQuery = true)
    void createIfMissing(UUID businessId, String period);

    /** Locked: the daily job and a client pressing "Pay" at the same moment cannot both pay the same period. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TaxObligation o where o.businessId = :businessId and o.period = :period")
    Optional<TaxObligation> findForUpdate(UUID businessId, String period);
}
