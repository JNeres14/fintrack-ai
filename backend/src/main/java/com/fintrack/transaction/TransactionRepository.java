package com.fintrack.transaction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Listagem com filtros e paginação entra no Bloco D. */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
}
