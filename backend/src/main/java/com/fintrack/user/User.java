package com.fintrack.user;

import com.fintrack.shared.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	protected User() {
	}

	/** O e-mail é normalizado (minúsculas, sem espaços nas pontas): o banco exige e-mail em minúsculas. */
	public User(String email, String passwordHash) {
		this.email = email.strip().toLowerCase();
		this.passwordHash = passwordHash;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public boolean isLocked(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	/** Conta uma falha; ao atingir o máximo, bloqueia até o instante dado e zera o contador. */
	public void registerFailedLogin(int maxAttempts, Instant lockUntil) {
		failedAttempts++;
		if (failedAttempts >= maxAttempts) {
			lockedUntil = lockUntil;
			failedAttempts = 0;
		}
	}

	public void clearFailedLogins() {
		failedAttempts = 0;
		lockedUntil = null;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
