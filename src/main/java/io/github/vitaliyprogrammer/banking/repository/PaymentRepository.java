package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByInitiatorIdAndIdempotencyKey(UUID initiatorId, String idempotencyKey);

    Optional<Payment> findByIdAndInitiatorId(UUID id, UUID initiatorId);
}
