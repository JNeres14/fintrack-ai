package com.fintrack.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fintrack.shared.FlowType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interface pública do módulo category para os demais módulos (a transação nunca usa o CategoryRepository). */
@Service
public class CategoryQueries {

	/** Visão mínima da categoria: a entidade JPA não sai do módulo. */
	public record CategoryRef(UUID id, String name, FlowType kind, UUID parentId) {
	}

	private final CategoryRepository categories;

	CategoryQueries(CategoryRepository categories) {
		this.categories = categories;
	}

	/** Vazio se a categoria não existe OU é de outro usuário. */
	@Transactional(readOnly = true)
	public Optional<CategoryRef> find(UUID userId, UUID id) {
		return categories.findByIdAndUserId(id, userId).map(c -> new CategoryRef(c.getId(), c.getName(), c.getKind(), c.getParentId()));
	}

	/** O id dado mais os das suas subcategorias (um nível). Vazio se a categoria não é do usuário. */
	@Transactional(readOnly = true)
	public List<UUID> idWithDescendants(UUID userId, UUID id) {
		if (categories.findByIdAndUserId(id, userId).isEmpty()) {
			return List.of();
		}
		List<UUID> ids = new ArrayList<>();
		ids.add(id);
		categories.findAllByUserIdAndParentId(userId, id).forEach(c -> ids.add(c.getId()));
		return ids;
	}
}
