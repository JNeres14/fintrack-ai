package com.fintrack.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fintrack.transaction.InvoicePaymentRepository.AccountSum;
import com.fintrack.transaction.TransactionRepository.AccountDelta;
import org.springframework.stereotype.Component;

/**
 * Saldo calculado (nunca armazenado), no SQL:
 * saldo inicial + receitas - despesas - pagamentos de fatura feitos com a conta + pagamentos recebidos pelo cartão
 * (ADR-016). Pagamento de fatura não é receita nem despesa: só aparece aqui.
 */
@Component
class AccountBalances {

	static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private final TransactionRepository transactions;
	private final InvoicePaymentRepository payments;

	AccountBalances(TransactionRepository transactions, InvoicePaymentRepository payments) {
		this.transactions = transactions;
		this.payments = payments;
	}

	/** Variação (sem o saldo inicial) de todas as contas do usuário, em uma consulta por origem de dado. */
	Map<UUID, BigDecimal> deltasByAccount(UUID userId) {
		Map<UUID, BigDecimal> deltas = new HashMap<>();
		for (AccountDelta d : transactions.sumDeltaByAccount(userId)) {
			deltas.merge(d.getAccountId(), d.getDelta(), BigDecimal::add);
		}
		for (AccountSum s : payments.sumReceivedByCard(userId)) {
			deltas.merge(s.getAccountId(), s.getTotal(), BigDecimal::add);
		}
		for (AccountSum s : payments.sumPaidBySource(userId)) {
			deltas.merge(s.getAccountId(), s.getTotal().negate(), BigDecimal::add);
		}
		return deltas;
	}

	BigDecimal balance(Account account, Map<UUID, BigDecimal> deltas) {
		return scale(account.getInitialBalance().add(deltas.getOrDefault(account.getId(), ZERO)));
	}

	BigDecimal balance(UUID userId, Account account) {
		return balance(account, deltasByAccount(userId));
	}

	/** Todos os valores vêm do banco com 2 casas, então não há arredondamento; UNNECESSARY garante isso. */
	static BigDecimal scale(BigDecimal value) {
		return value.setScale(2, RoundingMode.UNNECESSARY);
	}
}
