package com.fintrack.transaction;

import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, length = 100)
	private String name;

	protected PaymentMethod() {
	}

	public PaymentMethod(UUID userId, String name) {
		this.userId = userId;
		this.name = name;
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
}
