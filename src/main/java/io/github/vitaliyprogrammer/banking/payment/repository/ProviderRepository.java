package io.github.vitaliyprogrammer.banking.payment.repository;

import io.github.vitaliyprogrammer.banking.payment.entity.Provider;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    List<Provider> findAllByActiveTrueOrderByCategoryAscNameAsc();
}
