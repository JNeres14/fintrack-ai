package com.fintrack.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

	Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);

	List<PaymentMethod> findAllByUserIdOrderByName(UUID userId);
}
