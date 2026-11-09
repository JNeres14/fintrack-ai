package com.fintrack.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Limite mensal. {@code categoryId} nulo = orçamento geral do mês; {@code month} é sempre o dia 1. */
@Entity
@Table(name = "budgets")
public class Budget extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(name = "limit_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal limitAmount;

	@Column(nullable = false)
	private LocalDate month;

	protected Budget() {
	}

	public Budget(UUID userId, UUID categoryId, BigDecimal limitAmount, LocalDate month) {
		this.userId = userId;
		this.categoryId = categoryId;
		this.limitAmount = limitAmount;
		this.month = month.withDayOfMonth(1);
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public BigDecimal getLimitAmount() {
		return limitAmount;
	}

	public void setLimitAmount(BigDecimal limitAmount) {
		this.limitAmount = limitAmount;
	}

	public LocalDate getMonth() {
		return month;
	}
}
