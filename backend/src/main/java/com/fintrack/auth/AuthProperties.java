package com.fintrack.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bloqueio por tentativas, rate limiting e custo do BCrypt (ADR-011 e ADR-013). Valores não secretos. */
@ConfigurationProperties("app.auth")
public record AuthProperties(int maxFailedAttempts, int lockMinutes, int rateLimitPerMinute, int bcryptCost) {

	public AuthProperties {
		if (maxFailedAttempts < 1 || lockMinutes < 1 || rateLimitPerMinute < 1) {
			throw new IllegalStateException("Limites de autenticação devem ser positivos.");
		}
		if (bcryptCost < 4 || bcryptCost > 31) {
			throw new IllegalStateException("Custo do BCrypt deve estar entre 4 e 31.");
		}
	}
}
