package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fintrack.shared.BusinessRuleException;
import com.fintrack.shared.ConflictException;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.AccountDtos.AccountRequest;
import com.fintrack.transaction.AccountDtos.AccountResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Toda operação recebe o userId do token; conta de outro usuário se comporta como inexistente (404). */
@Service
class AccountService {

	private final AccountRepository accounts;
	private final TransactionRepository transactions;
	private final InvoicePaymentRepository payments;
	private final AccountBalances balances;

	AccountService(AccountRepository accounts, TransactionRepository transactions, InvoicePaymentRepository payments,
			AccountBalances balances) {
		this.accounts = accounts;
		this.transactions = transactions;
		this.payments = payments;
		this.balances = balances;
	}

	@Transactional(readOnly = true)
	List<AccountResponse> list(UUID userId) {
		Map<UUID, BigDecimal> deltas = balances.deltasByAccount(userId);
		return accounts.findAllByUserIdOrderByName(userId).stream()
				.map(a -> toResponse(a, balances.balance(a, deltas))).toList();
	}

	@Transactional(readOnly = true)
	AccountResponse get(UUID userId, UUID id) {
		Account account = find(userId, id);
		return toResponse(account, balances.balance(userId, account));
	}

	@Transactional
	AccountResponse create(UUID userId, AccountRequest req) {
		String name = req.name().strip();
		if (accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma conta com este nome.");
		}
		checkCardFields(req);
		Account account = new Account(userId, name, req.type(), scaled(req.initialBalance()));
		applyCardData(account, req);
		Account saved = accounts.saveAndFlush(account);
		return toResponse(saved, balances.balance(userId, saved));
	}

	@Transactional
	AccountResponse update(UUID userId, UUID id, AccountRequest req) {
		Account account = find(userId, id);
		String name = req.name().strip();
		if (!name.equalsIgnoreCase(account.getName()) && accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma conta com este nome.");
		}
		checkCardFields(req);
		checkCardChanges(userId, account, req);
		account.setName(name);
		account.setType(req.type());
		account.setInitialBalance(scaled(req.initialBalance()));
		applyCardData(account, req);
		accounts.saveAndFlush(account);
		return toResponse(account, balances.balance(userId, account));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		Account account = find(userId, id);
		if (transactions.existsByUserIdAndAccountId(userId, id)) {
			throw new ConflictException("A conta tem transações e não pode ser excluída.");
		}
		if (payments.existsByUserIdAndCardAccountId(userId, id) || payments.existsByUserIdAndSourceAccountId(userId, id)) {
			throw new ConflictException("A conta tem pagamentos de fatura e não pode ser excluída.");
		}
		accounts.delete(account);
	}

	/** Cartão exige limite e os dois dias; os outros tipos não podem tê-los (o banco também confere). */
	private static void checkCardFields(AccountRequest req) {
		boolean anyCardField = req.creditLimit() != null || req.closingDay() != null || req.dueDay() != null;
		if (req.type() == AccountType.CREDIT_CARD) {
			if (req.creditLimit() == null || req.closingDay() == null || req.dueDay() == null) {
				throw new BusinessRuleException(
						"Cartão de crédito exige limite, dia de fechamento e dia de vencimento.");
			}
		}
		else if (anyCardField) {
			throw new BusinessRuleException(
					"Limite e dias de fechamento e vencimento só valem para cartão de crédito.");
		}
	}

	/**
	 * Mudar o dia de fechamento reclassificaria compras entre faturas, inclusive as já pagas: bloqueado se o cartão
	 * tem pagamento. Mudar o tipo (cartão x outro) só é possível em conta sem transações nem pagamentos.
	 */
	private void checkCardChanges(UUID userId, Account account, AccountRequest req) {
		UUID id = account.getId();
		boolean willBeCard = req.type() == AccountType.CREDIT_CARD;
		if (account.isCreditCard() != willBeCard) {
			if (transactions.existsByUserIdAndAccountId(userId, id)
					|| payments.existsByUserIdAndCardAccountId(userId, id)
					|| payments.existsByUserIdAndSourceAccountId(userId, id)) {
				throw new ConflictException(
						"O tipo da conta não pode mudar entre cartão e outro tipo porque ela já tem movimentação.");
			}
		}
		else if (willBeCard && !req.closingDay().equals(account.getClosingDay())
				&& payments.existsByUserIdAndCardAccountId(userId, id)) {
			throw new ConflictException(
					"O dia de fechamento não pode mudar porque o cartão já tem pagamento de fatura.");
		}
	}

	private static void applyCardData(Account account, AccountRequest req) {
		if (req.type() == AccountType.CREDIT_CARD) {
			account.setCardData(req.creditLimit().setScale(2), req.closingDay(), req.dueDay());
		}
		else {
			account.setCardData(null, null, null);
		}
	}

	private Account find(UUID userId, UUID id) {
		return accounts.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
	}

	private static BigDecimal scaled(BigDecimal value) {
		return value == null ? AccountBalances.ZERO : value.setScale(2);
	}

	private static AccountResponse toResponse(Account a, BigDecimal balance) {
		return new AccountResponse(a.getId(), a.getName(), a.getType(), a.getInitialBalance(), balance,
				a.getCreditLimit(), a.getClosingDay(), a.getDueDay());
	}
}
