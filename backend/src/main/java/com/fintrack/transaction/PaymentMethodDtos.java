package com.fintrack.transaction;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class PaymentMethodDtos {

	private PaymentMethodDtos() {
	}

	record PaymentMethodRequest(@NotBlank @Size(max = 100) String name) {
	}

	record PaymentMethodResponse(UUID id, String name) {
	}
}
