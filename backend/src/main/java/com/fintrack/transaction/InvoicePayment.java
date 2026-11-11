package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Pagamento de fatura: registro próprio, não é receita nem despesa (ADR-016). */
@Entity
@Table(name = "invoice_payments")
class InvoicePayment extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "card_account_id", nullable = false, updatable = false)
	private UUID cardAccountId;

	@Column(name = "source_account_id", nullable = false, updatable = false)
	private UUID sourceAccountId;

	/** Sempre o dia 1 do mês de fechamento da fatura. */
	@Column(name = "invoice_month", nullable = false, updatable = false)
	private LocalDate invoiceMonth;

	@Column(nullable = false, updatable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "paid_on", nullable = false, updatable = false)
	private LocalDate paidOn;

	protected InvoicePayment() {
	}

	InvoicePayment(UUID userId, UUID cardAccountId, UUID sourceAccountId, LocalDate invoiceMonth, BigDecimal amount,
			LocalDate paidOn) {
		this.userId = userId;
		this.cardAccountId = cardAccountId;
		this.sourceAccountId = sourceAccountId;
		this.invoiceMonth = invoiceMonth;
		this.amount = amount;
		this.paidOn = paidOn;
	}

	UUID getCardAccountId() {
		return cardAccountId;
	}

	UUID getSourceAccountId() {
		return sourceAccountId;
	}

	LocalDate getInvoiceMonth() {
		return invoiceMonth;
	}

	BigDecimal getAmount() {
		return amount;
	}

	LocalDate getPaidOn() {
		return paidOn;
	}
}
