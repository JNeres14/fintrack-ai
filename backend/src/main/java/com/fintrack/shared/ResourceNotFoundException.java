package com.fintrack.shared;

/**
 * Recurso inexistente OU de outro usuário: a resposta é sempre 404, para não revelar que o id existe.
 * A mensagem não deve conter dados financeiros.
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
