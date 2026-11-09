package com.fintrack.transaction;

import java.util.List;
import java.util.UUID;

import com.fintrack.shared.CurrentUser;
import com.fintrack.transaction.PaymentMethodDtos.PaymentMethodRequest;
import com.fintrack.transaction.PaymentMethodDtos.PaymentMethodResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-methods")
class PaymentMethodController {

	private final PaymentMethodService service;
	private final CurrentUser currentUser;

	PaymentMethodController(PaymentMethodService service, CurrentUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	@GetMapping
	List<PaymentMethodResponse> list() {
		return service.list(currentUser.id());
	}

	@GetMapping("/{id}")
	PaymentMethodResponse get(@PathVariable UUID id) {
		return service.get(currentUser.id(), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PaymentMethodResponse create(@Valid @RequestBody PaymentMethodRequest body) {
		return service.create(currentUser.id(), body);
	}

	@PutMapping("/{id}")
	PaymentMethodResponse update(@PathVariable UUID id, @Valid @RequestBody PaymentMethodRequest body) {
		return service.update(currentUser.id(), id, body);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(currentUser.id(), id);
	}
}
