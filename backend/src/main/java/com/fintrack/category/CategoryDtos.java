package com.fintrack.category;

import java.util.UUID;

import com.fintrack.shared.FlowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class CategoryDtos {

	private CategoryDtos() {
	}

	/** parentId preenchido cria uma subcategoria (um nível só; o tipo deve ser o mesmo do pai). */
	record CategoryCreateRequest(
			@NotBlank @Size(max = 100) String name,
			@NotNull FlowType kind,
			UUID parentId) {
	}

	/** Só o nome muda: tipo e pai ficam fixos para não quebrar transações e orçamentos já feitos. */
	record CategoryUpdateRequest(@NotBlank @Size(max = 100) String name) {
	}

	record CategoryResponse(UUID id, String name, FlowType kind, UUID parentId) {
	}
}
