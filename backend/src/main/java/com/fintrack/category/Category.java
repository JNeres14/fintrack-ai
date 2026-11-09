package com.fintrack.category;

import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import com.fintrack.shared.FlowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private FlowType kind;

	/** Categoria pai (subcategoria quando preenchido). */
	@Column(name = "parent_id")
	private UUID parentId;

	protected Category() {
	}

	public Category(UUID userId, String name, FlowType kind, UUID parentId) {
		this.userId = userId;
		this.name = name;
		this.kind = kind;
		this.parentId = parentId;
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

	public FlowType getKind() {
		return kind;
	}

	public UUID getParentId() {
		return parentId;
	}
}
