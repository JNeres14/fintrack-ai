package com.fintrack.category;

import java.util.List;
import java.util.UUID;

import com.fintrack.category.CategoryDtos.CategoryCreateRequest;
import com.fintrack.category.CategoryDtos.CategoryResponse;
import com.fintrack.category.CategoryDtos.CategoryUpdateRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
class CategoryController {

	private final CategoryService service;
	private final CurrentUser currentUser;

	CategoryController(CategoryService service, CurrentUser currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	/** Lista plana; o cliente monta a árvore pelo parentId. */
	@GetMapping
	List<CategoryResponse> list() {
		return service.list(currentUser.id());
	}

	@GetMapping("/{id}")
	CategoryResponse get(@PathVariable UUID id) {
		return service.get(currentUser.id(), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CategoryResponse create(@Valid @RequestBody CategoryCreateRequest body) {
		return service.create(currentUser.id(), body);
	}

	@PutMapping("/{id}")
	CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest body) {
		return service.update(currentUser.id(), id, body);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(currentUser.id(), id);
	}
}
