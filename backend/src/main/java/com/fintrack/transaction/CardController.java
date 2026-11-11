package com.fintrack.transaction;

import java.util.List;
import java.util.UUID;

import com.fintrack.shared.CurrentUser;
import com.fintrack.transaction.CardDtos.CardResponse;
import com.fintrack.transaction.CardDtos.InvoiceDetail;
import com.fintrack.transaction.CardDtos.PaymentRequest;
import com.fintrack.transaction.CardDtos.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** O mês da fatura (AAAA-MM) é o mês do FECHAMENTO (ADR-016). */
@RestController
class CardController {

	private final CardService service;
	private final CurrentUser currentUser;

	CardController(CardService service, CurrentUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	@GetMapping("/api/cards")
	List<CardResponse> cards() {
		return service.list(currentUser.id());
	}

	@GetMapping("/api/accounts/{id}/invoices/{month}")
	InvoiceDetail invoice(@PathVariable UUID id, @PathVariable String month) {
		return service.invoice(currentUser.id(), id, month);
	}

	@PostMapping("/api/accounts/{id}/invoices/{month}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse pay(@PathVariable UUID id, @PathVariable String month, @Valid @RequestBody PaymentRequest body) {
		return service.pay(currentUser.id(), id, month, body);
	}

	@DeleteMapping("/api/accounts/{id}/invoices/{month}/payments/{paymentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePayment(@PathVariable UUID id, @PathVariable String month, @PathVariable UUID paymentId) {
		service.deletePayment(currentUser.id(), id, month, paymentId);
	}
}
