package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountType type;

	/** Pode ser negativo (cheque especial, fatura). O saldo atual é calculado, não armazenado. */
	@Column(name = "initial_balance", nullable = false, precision = 14, scale = 2)
	private BigDecimal initialBalance;

	protected Account() {
	}

	public Account(UUID userId, String name, AccountType type, BigDecimal initialBalance) {
		this.userId = userId;
		this.name = name;
		this.type = type;
		this.initialBalance = initialBalance;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AccountType getType() {
		return type;
	}

	public void setType(AccountType type) {
		this.type = type;
	}

	public BigDecimal getInitialBalance() {
		return initialBalance;
	}

	public void setInitialBalance(BigDecimal initialBalance) {
		this.initialBalance = initialBalance;
	}
}
