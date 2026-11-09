package com.fintrack.budget;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interface pública do módulo budget para os demais módulos (o CRUD de orçamentos entra no Bloco E). */
@Service
public class BudgetService {

	private final BudgetRepository budgets;

	BudgetService(BudgetRepository budgets) {
		this.budgets = budgets;
	}

	@Transactional(readOnly = true)
	public boolean categoryInUse(UUID userId, UUID categoryId) {
		return budgets.existsByUserIdAndCategoryId(userId, categoryId);
	}
}
