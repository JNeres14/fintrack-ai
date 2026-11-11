package com.fintrack.budget;

import java.util.List;
import java.util.UUID;

import com.fintrack.budget.BudgetDtos.BudgetCreateRequest;
import com.fintrack.budget.BudgetDtos.BudgetResponse;
import com.fintrack.budget.BudgetDtos.BudgetUpdateRequest;
import com.fintrack.shared.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
class BudgetController {

	private final BudgetService service;
	private final CurrentUser currentUser;

	BudgetController(BudgetService service, CurrentUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	/** month (AAAA-MM) é opcional; sem ele, usa o mês de hoje. */
	@GetMapping
	List<BudgetResponse> list(@RequestParam(required = false) String month) {
		return service.list(currentUser.id(), month);
	}

	@GetMapping("/{id}")
	BudgetResponse get(@PathVariable UUID id) {
		return service.get(currentUser.id(), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	BudgetResponse create(@Valid @RequestBody BudgetCreateRequest body) {
		return service.create(currentUser.id(), body);
	}

	@PutMapping("/{id}")
	BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody BudgetUpdateRequest body) {
		return service.update(currentUser.id(), id, body);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(currentUser.id(), id);
	}
}
