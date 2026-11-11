package com.fintrack.transaction;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fintrack.category.CategoryQueries;
import com.fintrack.category.CategoryQueries.CategoryRef;
import com.fintrack.shared.BusinessRuleException;
import com.fintrack.shared.FlowType;
import com.fintrack.shared.PageResponse;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.TransactionDtos.TransactionRequest;
import com.fintrack.transaction.TransactionDtos.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transações (RF-02, RF-05). Toda operação recebe o userId do token. Conta, categoria ou forma de pagamento de
 * outro usuário se comportam como inexistentes (404). Os métodos públicos são a interface para outros módulos.
 */
@Service
public class TransactionService {

	static final int DEFAULT_PAGE_SIZE = 20;
	static final int MAX_PAGE_SIZE = 100;
	static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

	/** Ordem fixa e estável: data decrescente; empates por criação e id, para a paginação não repetir nem pular. */
	private static final Sort ORDER = Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt"),
			Sort.Order.desc("id"));

	private final TransactionRepository transactions;
	private final AccountRepository accounts;
	private final PaymentMethodRepository paymentMethods;
	private final CategoryQueries categories;
	private final Clock clock;
	private final InvoiceGuard invoiceGuard;

	TransactionService(TransactionRepository transactions, AccountRepository accounts,
			PaymentMethodRepository paymentMethods, CategoryQueries categories, Clock clock,
			InvoiceGuard invoiceGuard) {
		this.transactions = transactions;
		this.accounts = accounts;
		this.paymentMethods = paymentMethods;
		this.categories = categories;
		this.clock = clock;
		this.invoiceGuard = invoiceGuard;
	}

	@Transactional(readOnly = true)
	public boolean categoryInUse(UUID userId, UUID categoryId) {
		return transactions.existsByUserIdAndCategoryId(userId, categoryId);
	}

	@Transactional(readOnly = true)
	PageResponse<TransactionResponse> list(UUID userId, LocalDate from, LocalDate to, UUID categoryId, FlowType type,
			UUID accountId, String text, int page, int size) {
		if (page < 0 || size < 1) {
			throw new BusinessRuleException("page deve ser 0 ou mais e size deve ser 1 ou mais.");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw new BusinessRuleException("A data inicial não pode ser depois da final.");
		}
		int effectiveSize = Math.min(size, MAX_PAGE_SIZE);
		List<UUID> categoryIds = null;
		if (categoryId != null) {
			// A categoria pai traz também as subcategorias. Categoria de outro usuário: lista vazia.
			categoryIds = categories.idWithDescendants(userId, categoryId);
			if (categoryIds.isEmpty()) {
				return PageResponse.empty(page, effectiveSize);
			}
		}
		Page<Transaction> result = transactions.findAll(
				TransactionSpecifications.filter(userId, from, to, categoryIds, type, accountId, text),
				PageRequest.of(page, effectiveSize, ORDER));
		return PageResponse.of(result, TransactionResponse::from);
	}

	@Transactional(readOnly = true)
	TransactionResponse get(UUID userId, UUID id) {
		return TransactionResponse.from(find(userId, id));
	}

	@Transactional
	TransactionResponse create(UUID userId, TransactionRequest req) {
		validate(userId, req);
		invoiceGuard.assertEditable(userId, req.accountId(), req.occurredOn());
		Transaction t = new Transaction(userId, req.accountId(), req.categoryId(), req.paymentMethodId(), req.type(),
				req.amount().setScale(2), req.occurredOn(), clean(req.merchant()), clean(req.description()));
		return TransactionResponse.from(transactions.saveAndFlush(t));
	}

	@Transactional
	TransactionResponse update(UUID userId, UUID id, TransactionRequest req) {
		Transaction t = find(userId, id);
		invoiceGuard.assertEditable(userId, t.getAccountId(), t.getOccurredOn()); // fatura de origem
		validate(userId, req);
		invoiceGuard.assertEditable(userId, req.accountId(), req.occurredOn()); // fatura de destino
		t.setAccountId(req.accountId());
		t.setCategoryId(req.categoryId());
		t.setPaymentMethodId(req.paymentMethodId());
		t.setType(req.type());
		t.setAmount(req.amount().setScale(2));
		t.setOccurredOn(req.occurredOn());
		t.setMerchant(clean(req.merchant()));
		t.setDescription(clean(req.description()));
		return TransactionResponse.from(transactions.saveAndFlush(t));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		Transaction t = find(userId, id);
		invoiceGuard.assertEditable(userId, t.getAccountId(), t.getOccurredOn());
		transactions.delete(t);
	}

	/** Único ponto de busca por id: toda leitura, edição e exclusão passa por aqui, sempre filtrando pelo usuário. */
	private Transaction find(UUID userId, UUID id) {
		return transactions.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));
	}

	private void validate(UUID userId, TransactionRequest req) {
		LocalDate max = LocalDate.now(clock).plusYears(1);
		if (req.occurredOn().isBefore(MIN_DATE) || req.occurredOn().isAfter(max)) {
			throw new BusinessRuleException("A data deve estar entre 01/01/2000 e um ano à frente de hoje.");
		}
		accounts.findByIdAndUserId(req.accountId(), userId)
				.orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
		CategoryRef category = categories.find(userId, req.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
		if (category.kind() != req.type()) {
			throw new BusinessRuleException("O tipo da transação deve ser o mesmo da categoria.");
		}
		if (req.paymentMethodId() != null) {
			paymentMethods.findByIdAndUserId(req.paymentMethodId(), userId)
					.orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento não encontrada."));
		}
	}

	private static String clean(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}
}
