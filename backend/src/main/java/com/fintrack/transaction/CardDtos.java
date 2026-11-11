package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fintrack.shared.Money;
import com.fintrack.transaction.TransactionDtos.TransactionResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

final class CardDtos {

	private CardDtos() {
	}

	/** Aberta: ainda recebe compras. Fechada: passou o fechamento e falta pagar. Paga: os pagamentos cobrem o total. */
	enum InvoiceStatus {
		OPEN, CLOSED, PAID
	}

	/** month é o mês do FECHAMENTO (AAAA-MM). total = despesas menos estornos no período. */
	record InvoiceSummary(String month, LocalDate periodStart, LocalDate closingDate, LocalDate dueDate,
			InvoiceStatus status, BigDecimal total, BigDecimal paid, BigDecimal remaining) {
	}

	/**
	 * usedLimit = dívida total do cartão (inclui compras com data futura e fatura não paga).
	 * availableLimit = limite - usado (pode ser negativo se estourou). currentInvoice é a aberta hoje;
	 * previousInvoice é a do mês anterior (normalmente a fechada a pagar).
	 */
	record CardResponse(UUID id, String name, BigDecimal creditLimit, BigDecimal usedLimit, BigDecimal availableLimit,
			BigDecimal balance, Integer closingDay, Integer dueDay, InvoiceSummary currentInvoice,
			InvoiceSummary previousInvoice) {
	}

	record InvoiceDetail(InvoiceSummary invoice, List<TransactionResponse> transactions,
			List<PaymentResponse> payments) {
	}

	/** Sem userId nem cartão no corpo: o cartão vem da URL e o dono, do token. paidOn vazio = hoje. */
	record PaymentRequest(
			@NotNull UUID sourceAccountId,
			@NotNull @Positive @Money BigDecimal amount,
			LocalDate paidOn) {
	}

	record PaymentResponse(UUID id, UUID cardAccountId, UUID sourceAccountId, String invoiceMonth, BigDecimal amount,
			LocalDate paidOn) {
	}
}
