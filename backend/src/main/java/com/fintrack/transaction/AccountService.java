package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fintrack.shared.ConflictException;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.AccountDtos.AccountRequest;
import com.fintrack.transaction.AccountDtos.AccountResponse;
import com.fintrack.transaction.TransactionRepository.AccountDelta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Toda operação recebe o userId do token; conta de outro usuário se comporta como inexistente (404). */
@Service
class AccountService {

	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private final AccountRepository accounts;
	private final TransactionRepository transactions;

	AccountService(AccountRepository accounts, TransactionRepository transactions) {
		this.accounts = accounts;
		this.transactions = transactions;
	}

	@Transactional(readOnly = true)
	List<AccountResponse> list(UUID userId) {
		Map<UUID, BigDecimal> deltas = new HashMap<>();
		for (AccountDelta d : transactions.sumDeltaByAccount(userId)) {
			deltas.put(d.getAccountId(), d.getDelta());
		}
		return accounts.findAllByUserIdOrderByName(userId).stream()
				.map(a -> toResponse(a, deltas.getOrDefault(a.getId(), ZERO))).toList();
	}

	@Transactional(readOnly = true)
	AccountResponse get(UUID userId, UUID id) {
		Account account = find(userId, id);
		return toResponse(account, transactions.sumDeltaForAccount(userId, id));
	}

	@Transactional
	AccountResponse create(UUID userId, AccountRequest req) {
		String name = req.name().strip();
		if (accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma conta com este nome.");
		}
		Account saved = accounts.saveAndFlush(new Account(userId, name, req.type(), scaled(req.initialBalance())));
		return toResponse(saved, ZERO);
	}

	@Transactional
	AccountResponse update(UUID userId, UUID id, AccountRequest req) {
		Account account = find(userId, id);
		String name = req.name().strip();
		if (!name.equalsIgnoreCase(account.getName()) && accounts.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma conta com este nome.");
		}
		account.setName(name);
		account.setType(req.type());
		account.setInitialBalance(scaled(req.initialBalance()));
		accounts.saveAndFlush(account);
		return toResponse(account, transactions.sumDeltaForAccount(userId, id));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		Account account = find(userId, id);
		if (transactions.existsByUserIdAndAccountId(userId, id)) {
			throw new ConflictException("A conta tem transações e não pode ser excluída.");
		}
		accounts.delete(account);
	}

	private Account find(UUID userId, UUID id) {
		return accounts.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
	}

	private static BigDecimal scaled(BigDecimal value) {
		return value == null ? ZERO : value.setScale(2);
	}

	private static AccountResponse toResponse(Account a, BigDecimal delta) {
		return new AccountResponse(a.getId(), a.getName(), a.getType(), a.getInitialBalance(),
				a.getInitialBalance().add(delta));
	}
}
