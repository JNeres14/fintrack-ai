package com.fintrack.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByIdAndUserId(UUID id, UUID userId);

	List<Category> findAllByUserIdOrderByName(UUID userId);
}
