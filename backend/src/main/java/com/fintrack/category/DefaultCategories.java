package com.fintrack.category;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fintrack.shared.FlowType;
import com.fintrack.user.UserRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Categorias padrão de cada novo usuário (RF-03). Roda dentro da transação do cadastro. */
@Component
class DefaultCategories {

	/** Despesas: categoria -> subcategorias (lista vazia = sem subcategorias). Ordem preservada. */
	static final Map<String, List<String>> EXPENSES = new java.util.LinkedHashMap<>();

	static {
		EXPENSES.put("Alimentação", List.of("Mercado", "Restaurantes"));
		EXPENSES.put("Transporte", List.of("Combustível", "Transporte público e apps"));
		EXPENSES.put("Moradia", List.of("Aluguel", "Contas da casa"));
		EXPENSES.put("Saúde", List.of());
		EXPENSES.put("Educação", List.of());
		EXPENSES.put("Lazer", List.of());
		EXPENSES.put("Compras", List.of());
		EXPENSES.put("Assinaturas", List.of());
		EXPENSES.put("Outros", List.of());
	}

	static final List<String> INCOMES = List.of("Salário", "Rendimentos", "Outros");

	private final CategoryRepository categories;

	DefaultCategories(CategoryRepository categories) {
		this.categories = categories;
	}

	@EventListener
	void onUserRegistered(UserRegistered event) {
		UUID userId = event.userId();
		EXPENSES.forEach((name, children) -> {
			Category parent = categories.save(new Category(userId, name, FlowType.EXPENSE, null));
			children.forEach(child -> categories.save(new Category(userId, child, FlowType.EXPENSE, parent.getId())));
		});
		INCOMES.forEach(name -> categories.save(new Category(userId, name, FlowType.INCOME, null)));
	}
}
