package com.fintrack.shared;

/** Regra de negócio que impede a operação por conflito com o estado atual (nome repetido, item em uso). Vira 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
