package com.fintrack.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fintrack.shared.FlowType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	Optional<Category> findByIdAndUserId(UUID id, UUID userId);

	List<Category> findAllByUserIdOrderByName(UUID userId);

	List<Category> findAllByUserIdAndParentId(UUID userId, UUID parentId);

	/** Nome único entre as categorias raiz do mesmo tipo (sem diferenciar maiúsculas). */
	boolean existsByUserIdAndKindAndNameIgnoreCaseAndParentIdIsNull(UUID userId, FlowType kind, String name);

	/** Nome único entre as subcategorias do mesmo pai. */
	boolean existsByUserIdAndKindAndNameIgnoreCaseAndParentId(UUID userId, FlowType kind, String name, UUID parentId);

	boolean existsByUserIdAndParentId(UUID userId, UUID parentId);
}
