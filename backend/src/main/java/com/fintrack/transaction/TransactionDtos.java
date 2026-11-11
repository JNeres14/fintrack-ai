package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.shared.FlowType;
import com.fintrack.shared.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

final class TransactionDtos {

	private TransactionDtos() {
	}

	/** Sem userId: o dono é sempre o do token. O valor é sempre positivo; o sinal vem do tipo. */
	record TransactionRequest(
			@NotNull UUID accountId,
			@NotNull UUID categoryId,
			UUID paymentMethodId,
			@NotNull FlowType type,
			@NotNull @Positive @Money BigDecimal amount,
			@NotNull LocalDate occurredOn,
			@Size(max = 200) String merchant,
			@Size(max = 500) String description) {
	}

	record TransactionResponse(UUID id, UUID accountId, UUID categoryId, UUID paymentMethodId, FlowType type,
			BigDecimal amount, LocalDate occurredOn, String merchant, String description) {

		static TransactionResponse from(Transaction t) {
			return new TransactionResponse(t.getId(), t.getAccountId(), t.getCategoryId(), t.getPaymentMethodId(),
					t.getType(), t.getAmount(), t.getOccurredOn(), t.getMerchant(), t.getDescription());
		}
	}
}
