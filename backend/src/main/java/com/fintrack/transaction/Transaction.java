package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import com.fintrack.shared.FlowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Lançamento financeiro. As chaves estrangeiras são guardadas como UUID (sem associação JPA) para evitar
 * carregamento preguiçoso e consultas N+1; o banco garante que conta, categoria e forma de pagamento
 * pertencem ao mesmo usuário.
 */
@Entity
@Table(name = "transactions")
public class Transaction extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "account_id", nullable = false)
	private UUID accountId;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(name = "payment_method_id")
	private UUID paymentMethodId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private FlowType type;

	/** Sempre positivo; o sinal vem de {@code type}. */
	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "occurred_on", nullable = false)
	private LocalDate occurredOn;

	@Column(length = 200)
	private String merchant;

	@Column(length = 500)
	private String description;

	protected Transaction() {
	}

	public Transaction(UUID userId, UUID accountId, UUID categoryId, UUID paymentMethodId, FlowType type,
			BigDecimal amount, LocalDate occurredOn, String merchant, String description) {
		this.userId = userId;
		this.accountId = accountId;
		this.categoryId = categoryId;
		this.paymentMethodId = paymentMethodId;
		this.type = type;
		this.amount = amount;
		this.occurredOn = occurredOn;
		this.merchant = merchant;
		this.description = description;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

	public UUID getPaymentMethodId() {
		return paymentMethodId;
	}

	public void setPaymentMethodId(UUID paymentMethodId) {
		this.paymentMethodId = paymentMethodId;
	}

	public FlowType getType() {
		return type;
	}

	public void setType(FlowType type) {
		this.type = type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getOccurredOn() {
		return occurredOn;
	}

	public void setOccurredOn(LocalDate occurredOn) {
		this.occurredOn = occurredOn;
	}

	public String getMerchant() {
		return merchant;
	}

	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
