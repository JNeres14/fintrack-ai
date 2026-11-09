package com.fintrack.shared;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Único ponto que entrega o userId autenticado (regra inegociável 2). O id sai do "sub" do JWT já validado
 * (assinatura, emissor e expiração); nenhum endpoint recebe userId do cliente.
 */
@Component
public class CurrentUser {

	public UUID id() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof JwtAuthenticationToken jwt) {
			try {
				return UUID.fromString(jwt.getToken().getSubject());
			}
			catch (IllegalArgumentException | NullPointerException e) {
				throw new AuthenticationCredentialsNotFoundException("Token sem usuário válido");
			}
		}
		throw new AuthenticationCredentialsNotFoundException("Nenhum usuário autenticado");
	}
}
