package com.fintrack.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interface pública do módulo user: outros módulos (auth) nunca usam o UserRepository diretamente. */
@Service
public class UserService {

	private final UserRepository users;

	UserService(UserRepository users) {
		this.users = users;
	}

	/** O e-mail é normalizado do mesmo jeito em todo lugar (minúsculas, sem espaços nas pontas). */
	public static String normalizeEmail(String email) {
		return email.strip().toLowerCase();
	}

	@Transactional(readOnly = true)
	public boolean emailExists(String email) {
		return users.existsByEmail(normalizeEmail(email));
	}

	@Transactional
	public UserView create(String email, String passwordHash) {
		return UserView.from(users.save(new User(email, passwordHash)));
	}

	@Transactional(readOnly = true)
	public Optional<UserView> findByEmail(String email) {
		return users.findByEmail(normalizeEmail(email)).map(UserView::from);
	}

	/** Registra a falha em transação própria, para valer mesmo quando o login em seguida responde 401. */
	@Transactional
	public void registerFailedLogin(UUID userId, int maxAttempts, Duration lockDuration, Instant now) {
		users.findById(userId).ifPresent(u -> u.registerFailedLogin(maxAttempts, now.plus(lockDuration)));
	}

	@Transactional
	public void clearFailedLogins(UUID userId) {
		users.findById(userId).ifPresent(User::clearFailedLogins);
	}

	/** Visão imutável do usuário para outros módulos (a entidade JPA não sai do módulo). */
	public record UserView(UUID id, String email, String passwordHash, Instant lockedUntil) {

		static UserView from(User u) {
			return new UserView(u.getId(), u.getEmail(), u.getPasswordHash(), u.getLockedUntil());
		}

		public boolean isLocked(Instant now) {
			return lockedUntil != null && lockedUntil.isAfter(now);
		}
	}
}
