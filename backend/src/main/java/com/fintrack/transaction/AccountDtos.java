package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.UUID;

import com.fintrack.shared.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

final class AccountDtos {

	private AccountDtos() {
	}

	/**
	 * O userId nunca vem no corpo: mesmo que o cliente envie, o campo é ignorado.
	 * creditLimit, closingDay e dueDay: obrigatórios para CREDIT_CARD e proibidos nos outros tipos (ADR-016).
	 */
	record AccountRequest(
			@NotBlank @Size(max = 100) String name,
			@NotNull AccountType type,
			@Money BigDecimal initialBalance,
			@Money @PositiveOrZero BigDecimal creditLimit,
			@Min(1) @Max(28) Integer closingDay,
			@Min(1) @Max(28) Integer dueDay) {
	}

	/** balance = saldo inicial + receitas - despesas - pagamentos feitos + pagamentos recebidos, calculado no backend. */
	record AccountResponse(UUID id, String name, AccountType type, BigDecimal initialBalance, BigDecimal balance,
			BigDecimal creditLimit, Integer closingDay, Integer dueDay) {
	}
}
