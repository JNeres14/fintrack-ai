package com.fintrack.auth;

import java.time.Instant;
import java.util.UUID;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Só o hash SHA-256 do refresh token é guardado; o valor original só existe na resposta ao cliente. */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken extends AuditableEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "family_id", nullable = false, updatable = false)
	private UUID familyId;

	@Column(name = "token_hash", nullable = false, updatable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected RefreshToken() {
	}

	RefreshToken(UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
		this.userId = userId;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	UUID getUserId() {
		return userId;
	}

	UUID getFamilyId() {
		return familyId;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	Instant getUsedAt() {
		return usedAt;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}
}
