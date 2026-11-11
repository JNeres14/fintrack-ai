package com.fintrack.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Toda consulta recebe o userId (vindo do token). month é sempre o dia 1 do mês. */
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

	List<Budget> findAllByUserIdAndMonth(UUID userId, LocalDate month);

	boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);

	/** Orçamento geral do mês (sem categoria). */
	boolean existsByUserIdAndMonthAndCategoryIdIsNull(UUID userId, LocalDate month);

	boolean existsByUserIdAndMonthAndCategoryId(UUID userId, LocalDate month, UUID categoryId);
}
