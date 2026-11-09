package com.fintrack.category;

import java.util.List;
import java.util.UUID;

import com.fintrack.budget.BudgetService;
import com.fintrack.category.CategoryDtos.CategoryCreateRequest;
import com.fintrack.category.CategoryDtos.CategoryResponse;
import com.fintrack.category.CategoryDtos.CategoryUpdateRequest;
import com.fintrack.shared.BusinessRuleException;
import com.fintrack.shared.ConflictException;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Usa transações e orçamentos só pelos services públicos desses módulos, nunca pelos repositories deles. */
@Service
class CategoryService {

	private final CategoryRepository categories;
	private final TransactionService transactions;
	private final BudgetService budgets;

	CategoryService(CategoryRepository categories, TransactionService transactions, BudgetService budgets) {
		this.categories = categories;
		this.transactions = transactions;
		this.budgets = budgets;
	}

	@Transactional(readOnly = true)
	List<CategoryResponse> list(UUID userId) {
		return categories.findAllByUserIdOrderByName(userId).stream().map(CategoryService::toResponse).toList();
	}

	@Transactional(readOnly = true)
	CategoryResponse get(UUID userId, UUID id) {
		return toResponse(find(userId, id));
	}

	@Transactional
	CategoryResponse create(UUID userId, CategoryCreateRequest req) {
		String name = req.name().strip();
		if (req.parentId() != null) {
			Category parent = find(userId, req.parentId()); // pai de outro usuário = 404
			if (parent.getParentId() != null) {
				throw new BusinessRuleException("Uma subcategoria não pode ter subcategorias.");
			}
			if (parent.getKind() != req.kind()) {
				throw new BusinessRuleException("A subcategoria deve ter o mesmo tipo da categoria pai.");
			}
		}
		if (nameTaken(userId, req.kind(), name, req.parentId())) {
			throw new ConflictException("Já existe uma categoria com este nome neste nível.");
		}
		return toResponse(categories.saveAndFlush(new Category(userId, name, req.kind(), req.parentId())));
	}

	@Transactional
	CategoryResponse update(UUID userId, UUID id, CategoryUpdateRequest req) {
		Category category = find(userId, id);
		String name = req.name().strip();
		if (!name.equalsIgnoreCase(category.getName())
				&& nameTaken(userId, category.getKind(), name, category.getParentId())) {
			throw new ConflictException("Já existe uma categoria com este nome neste nível.");
		}
		category.setName(name);
		return toResponse(categories.saveAndFlush(category));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		Category category = find(userId, id);
		if (categories.existsByUserIdAndParentId(userId, id)) {
			throw new ConflictException("A categoria tem subcategorias e não pode ser excluída.");
		}
		if (transactions.categoryInUse(userId, id)) {
			throw new ConflictException("A categoria tem transações e não pode ser excluída.");
		}
		if (budgets.categoryInUse(userId, id)) {
			throw new ConflictException("A categoria tem orçamentos e não pode ser excluída.");
		}
		categories.delete(category);
	}

	private boolean nameTaken(UUID userId, com.fintrack.shared.FlowType kind, String name, UUID parentId) {
		return parentId == null
				? categories.existsByUserIdAndKindAndNameIgnoreCaseAndParentIdIsNull(userId, kind, name)
				: categories.existsByUserIdAndKindAndNameIgnoreCaseAndParentId(userId, kind, name, parentId);
	}

	private Category find(UUID userId, UUID id) {
		return categories.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
	}

	private static CategoryResponse toResponse(Category c) {
		return new CategoryResponse(c.getId(), c.getName(), c.getKind(), c.getParentId());
	}
}
