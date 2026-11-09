package com.fintrack.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Política de senha (ADR-011): comprimento vale mais que complexidade.
 * Mínimo 12 caracteres; máximo 72 bytes (o BCrypt ignora o que passa disso, então recusamos em vez de truncar);
 * não pode ser o e-mail nem uma senha muito comum.
 */
@Component
class PasswordPolicy {

	static final int MIN_LENGTH = 12;
	static final int MAX_BYTES = 72;

	private static final Set<String> COMMON = Set.of("123456789012", "1234567890123", "password1234",
			"passw0rd1234", "qwertyuiop12", "qwertyuiopas", "senha1234567", "senhasenha123", "abcdefghijkl",
			"111111111111", "000000000000", "aaaaaaaaaaaa", "123123123123", "admin1234567");

	/** Devolve a mensagem de erro, ou vazio se a senha é aceitável. */
	Optional<String> validate(String password, String email) {
		if (password == null || password.codePointCount(0, password.length()) < MIN_LENGTH) {
			return Optional.of("A senha deve ter pelo menos " + MIN_LENGTH + " caracteres.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			return Optional.of("A senha é longa demais (máximo de " + MAX_BYTES + " bytes).");
		}
		String lower = password.toLowerCase();
		if (COMMON.contains(lower) || lower.equals(email.strip().toLowerCase())) {
			return Optional.of("Essa senha é fácil de adivinhar. Escolha outra.");
		}
		return Optional.empty();
	}
}
