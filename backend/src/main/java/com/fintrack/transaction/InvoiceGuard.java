package com.fintrack.transaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import com.fintrack.shared.ConflictException;
import org.springframework.stereotype.Component;

/**
 * Impede criar, editar ou excluir compra de fatura que já tem pagamento, parcial ou total (ADR-016, seção 5).
 * Único ponto da regra: o TransactionService chama aqui antes de gravar. Deve rodar dentro da transação do chamador.
 */
@Component
class InvoiceGuard {

	private final AccountRepository accounts;
	private final InvoicePaymentRepository payments;

	InvoiceGuard(AccountRepository accounts, InvoicePaymentRepository payments) {
		this.accounts = accounts;
		this.payments = payments;
	}

	void assertEditable(UUID userId, UUID accountId, LocalDate purchaseDate) {
		Account account = accounts.findByIdAndUserId(accountId, userId).orElse(null);
		if (account == null || !account.isCreditCard()) {
			return; // conta comum não tem fatura
		}
		// Trava a conta até o fim da transação: um pagamento simultâneo espera, e depois enxerga a compra gravada.
		accounts.findByIdAndUserIdForUpdate(accountId, userId);
		YearMonth invoice = new InvoiceCalendar(account.getClosingDay(), account.getDueDay()).invoiceOf(purchaseDate);
		if (payments.existsByUserIdAndCardAccountIdAndInvoiceMonth(userId, accountId, invoice.atDay(1))) {
			throw new ConflictException("A fatura desta compra já tem pagamento registrado e não pode ser alterada.");
		}
	}
}
