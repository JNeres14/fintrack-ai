package com.fintrack.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

	List<Budget> findAllByUserIdAndMonth(UUID userId, LocalDate month);
}
