package io.github.vitaliyprogrammer.banking.business.repository;

import io.github.vitaliyprogrammer.banking.business.entity.BusinessIncome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessIncomeRepository extends JpaRepository<BusinessIncome, UUID> {

    List<BusinessIncome> findTop20ByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    @Query("select coalesce(sum(i.amount), 0) from BusinessIncome i where i.businessId = :businessId and i.createdAt >= :since")
    BigDecimal sumIncomeSince(UUID businessId, Instant since);

    @Query("select coalesce(sum(i.tax), 0) from BusinessIncome i where i.businessId = :businessId and i.createdAt >= :since")
    BigDecimal sumTaxSince(UUID businessId, Instant since);

    @Query("""
            select coalesce(sum(i.tax), 0) from BusinessIncome i
            where i.businessId = :businessId and i.createdAt >= :from and i.createdAt < :to""")
    BigDecimal sumTaxBetween(UUID businessId, Instant from, Instant to);
}
