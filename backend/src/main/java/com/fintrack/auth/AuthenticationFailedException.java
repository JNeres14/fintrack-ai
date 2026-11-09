package com.fintrack.auth;

/**
 * Credencial ou refresh token inválido. A mensagem é sempre genérica: não revela o motivo
 * (e-mail inexistente, senha errada, conta bloqueada...).
 */
class AuthenticationFailedException extends RuntimeException {

	AuthenticationFailedException() {
		super("Credenciais inválidas");
	}
}
