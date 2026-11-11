package com.fintrack.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fintrack.budget.BudgetDtos.BudgetCreateRequest;
import com.fintrack.budget.BudgetDtos.BudgetResponse;
import com.fintrack.budget.BudgetDtos.BudgetStatus;
import com.fintrack.budget.BudgetDtos.BudgetUpdateRequest;
import com.fintrack.category.CategoryQueries;
import com.fintrack.category.CategoryQueries.CategoryRef;
import com.fintrack.shared.AppToday;
import com.fintrack.shared.BusinessRuleException;
import com.fintrack.shared.ConflictException;
import com.fintrack.shared.FlowType;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orçamentos mensais (RF-20, ADR-018). O consumo é calculado no banco a cada consulta, nunca no cliente.
 * Usa transações e categorias só pelas interfaces públicas desses módulos. Todo método recebe o userId do token.
 */
@Service
public class BudgetService {

	private static final YearMonth MIN_MONTH = YearMonth.of(2000, 1);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal WARNING_PERCENT = BigDecimal.valueOf(80);

	private final BudgetRepository budgets;
	private final TransactionService transactions;
	private final CategoryQueries categories;
	private final AppToday today;

	BudgetService(BudgetRepository budgets, TransactionService transactions, CategoryQueries categories,
			AppToday today) {
		this.budgets = budgets;
		this.transactions = transactions;
		this.categories = categories;
		this.today = today;
	}

	/** Interface pública para o módulo category: categoria com orçamento não pode ser excluída. */
	@Transactional(readOnly = true)
	public boolean categoryInUse(UUID userId, UUID categoryId) {
		return budgets.existsByUserIdAndCategoryId(userId, categoryId);
	}

	/** monthText vazio = mês de hoje. Geral primeiro; depois por nome da categoria. */
	@Transactional(readOnly = true)
	List<BudgetResponse> list(UUID userId, String monthText) {
		YearMonth month = monthText == null || monthText.isBlank() ? YearMonth.from(today.today())
				: parseMonth(monthText);
		return budgets.findAllByUserIdAndMonth(userId, month.atDay(1)).stream().map(b -> toResponse(userId, b))
				.sorted(Comparator.comparing((BudgetResponse r) -> r.categoryId() != null)
						.thenComparing(r -> r.categoryName() == null ? "" : r.categoryName().toLowerCase()))
				.toList();
	}

	@Transactional(readOnly = true)
	BudgetResponse get(UUID userId, UUID id) {
		return toResponse(userId, find(userId, id));
	}

	@Transactional
	BudgetResponse create(UUID userId, BudgetCreateRequest req) {
		LocalDate month = parseMonth(req.month()).atDay(1);
		if (req.categoryId() != null) {
			CategoryRef category = categories.find(userId, req.categoryId())
					.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
			if (category.kind() != FlowType.EXPENSE) {
				throw new BusinessRuleException("Só categorias de despesa podem ter orçamento.");
			}
		}
		boolean exists = req.categoryId() == null
				? budgets.existsByUserIdAndMonthAndCategoryIdIsNull(userId, month)
				: budgets.existsByUserIdAndMonthAndCategoryId(userId, month, req.categoryId());
		if (exists) {
			throw new ConflictException("Já existe um orçamento para este mês" + (req.categoryId() == null ? "." : " e categoria."));
		}
		Budget saved = budgets.saveAndFlush(new Budget(userId, req.categoryId(), req.limit().setScale(2), month));
		return toResponse(userId, saved);
	}

	@Transactional
	BudgetResponse update(UUID userId, UUID id, BudgetUpdateRequest req) {
		Budget budget = find(userId, id);
		budget.setLimitAmount(req.limit().setScale(2));
		return toResponse(userId, budgets.saveAndFlush(budget));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		budgets.delete(find(userId, id));
	}

	private Budget find(UUID userId, UUID id) {
		return budgets.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado."));
	}

	private BudgetResponse toResponse(UUID userId, Budget b) {
		YearMonth month = YearMonth.from(b.getMonth());
		CategoryRef category = null;
		BigDecimal spent;
		if (b.getCategoryId() == null) {
			spent = transactions.expensesTotal(userId, null, month.atDay(1), month.atEndOfMonth());
		}
		else {
			category = categories.find(userId, b.getCategoryId()).orElse(null);
			// a categoria pai conta também as despesas das subcategorias
			spent = transactions.expensesTotal(userId, categories.idWithDescendants(userId, b.getCategoryId()),
					month.atDay(1), month.atEndOfMonth());
		}
		BigDecimal limit = b.getLimitAmount();
		BigDecimal percent = spent.multiply(HUNDRED).divide(limit, 1, RoundingMode.HALF_UP);
		return new BudgetResponse(b.getId(), month.toString(), b.getCategoryId(),
				category == null ? null : category.name(), limit, spent, limit.subtract(spent), percent,
				status(spent, limit));
	}

	/** Compara os valores exatos (sem passar pelo percentual arredondado): 79,96% não vira 80%. */
	static BudgetStatus status(BigDecimal spent, BigDecimal limit) {
		if (spent.compareTo(limit) > 0) {
			return BudgetStatus.EXCEEDED;
		}
		return spent.multiply(HUNDRED).compareTo(limit.multiply(WARNING_PERCENT)) >= 0 ? BudgetStatus.WARNING
				: BudgetStatus.OK;
	}

	private YearMonth parseMonth(String text) {
		YearMonth month;
		try {
			month = YearMonth.parse(text.strip());
		}
		catch (DateTimeParseException e) {
			throw new BusinessRuleException("Mês inválido: use o formato AAAA-MM.");
		}
		if (month.isBefore(MIN_MONTH) || month.isAfter(YearMonth.from(today.today()).plusYears(1))) {
			throw new BusinessRuleException("Mês fora do intervalo permitido.");
		}
		return month;
	}
}
