package com.fintrack.transaction;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interface pública do módulo transaction para os demais módulos (o CRUD de transações entra no Bloco D). */
@Service
public class TransactionService {

	private final TransactionRepository transactions;

	TransactionService(TransactionRepository transactions) {
		this.transactions = transactions;
	}

	@Transactional(readOnly = true)
	public boolean categoryInUse(UUID userId, UUID categoryId) {
		return transactions.existsByUserIdAndCategoryId(userId, categoryId);
	}
}
