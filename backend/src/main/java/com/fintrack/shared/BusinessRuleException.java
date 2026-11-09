package com.fintrack.shared;

/** Pedido válido no formato, mas que quebra uma regra de negócio (ex.: subcategoria de subcategoria). Vira 400. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
