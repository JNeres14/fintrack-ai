package com.fintrack.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Toda consulta recebe o userId (vindo do token, nunca do cliente). */
public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByIdAndUserId(UUID id, UUID userId);

	List<Account> findAllByUserIdOrderByName(UUID userId);
}
