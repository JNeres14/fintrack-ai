package com.fintrack.auth;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT (ADR-010). O segredo vem só de variável de ambiente, sem valor padrão:
 * se faltar, estiver vazio ou for curto, a aplicação não sobe. A mensagem de erro nunca traz o segredo.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, int accessTtlMinutes, int refreshTtlDays) {

	static final int MIN_SECRET_BYTES = 32;
	static final int MAX_ACCESS_TTL_MINUTES = 15;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET ausente ou curto demais: use pelo menos " + MIN_SECRET_BYTES + " bytes aleatórios.");
		}
		if (accessTtlMinutes < 1 || accessTtlMinutes > MAX_ACCESS_TTL_MINUTES) {
			throw new IllegalStateException(
					"JWT_ACCESS_TTL_MINUTES deve estar entre 1 e " + MAX_ACCESS_TTL_MINUTES + " (RNF-02).");
		}
		if (refreshTtlDays < 1) {
			throw new IllegalStateException("JWT_REFRESH_TTL_DAYS deve ser pelo menos 1.");
		}
	}

	/** Evita que o segredo apareça se alguém logar o objeto sem querer. */
	@Override
	public String toString() {
		return "JwtProperties[secret=***, accessTtlMinutes=" + accessTtlMinutes + ", refreshTtlDays="
				+ refreshTtlDays + "]";
	}
}
