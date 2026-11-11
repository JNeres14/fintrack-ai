package com.fintrack.budget;

import java.math.BigDecimal;
import java.util.UUID;

import com.fintrack.shared.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

final class BudgetDtos {

	private BudgetDtos() {
	}

	/** month no formato AAAA-MM. categoryId vazio = orçamento geral do mês. Sem userId: o dono é o do token. */
	record BudgetCreateRequest(
			@NotBlank String month,
			UUID categoryId,
			@NotNull @Positive @Money BigDecimal limit) {
	}

	/** Só o limite muda; mês e categoria ficam fixos (para mudar, exclua e crie de novo). */
	record BudgetUpdateRequest(@NotNull @Positive @Money BigDecimal limit) {
	}

	/** OK: abaixo de 80% do limite. WARNING: de 80% até o limite. EXCEEDED: gasto acima do limite. */
	enum BudgetStatus {
		OK, WARNING, EXCEEDED
	}

	/**
	 * Consumo calculado no backend: spent = despesas do mês (pela data da compra; a categoria pai inclui as
	 * subcategorias); remaining pode ser negativo; percentUsed tem 1 casa e pode passar de 100.
	 */
	record BudgetResponse(UUID id, String month, UUID categoryId, String categoryName, BigDecimal limit,
			BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed, BudgetStatus status) {
	}
}
