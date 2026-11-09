package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.UUID;

import com.fintrack.shared.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class AccountDtos {

	private AccountDtos() {
	}

	/** O userId nunca vem no corpo: mesmo que o cliente envie, o campo é ignorado. */
	record AccountRequest(
			@NotBlank @Size(max = 100) String name,
			@NotNull AccountType type,
			@Money BigDecimal initialBalance) {
	}

	/** balance = saldo inicial + receitas - despesas, calculado no backend a cada consulta. */
	record AccountResponse(UUID id, String name, AccountType type, BigDecimal initialBalance, BigDecimal balance) {
	}
}
