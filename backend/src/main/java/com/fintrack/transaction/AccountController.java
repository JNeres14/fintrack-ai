package com.fintrack.transaction;

import java.util.List;
import java.util.UUID;

import com.fintrack.shared.CurrentUser;
import com.fintrack.transaction.AccountDtos.AccountRequest;
import com.fintrack.transaction.AccountDtos.AccountResponse;
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
@RequestMapping("/api/accounts")
class AccountController {

	private final AccountService service;
	private final CurrentUser currentUser;

	AccountController(AccountService service, CurrentUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	@GetMapping
	List<AccountResponse> list() {
		return service.list(currentUser.id());
	}

	@GetMapping("/{id}")
	AccountResponse get(@PathVariable UUID id) {
		return service.get(currentUser.id(), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AccountResponse create(@Valid @RequestBody AccountRequest body) {
		return service.create(currentUser.id(), body);
	}

	@PutMapping("/{id}")
	AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRequest body) {
		return service.update(currentUser.id(), id, body);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(currentUser.id(), id);
	}
}
