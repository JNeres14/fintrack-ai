package com.fintrack.shared;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Resposta paginada da API (formato próprio e estável, sem expor o JSON interno do Spring). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	public static <T> PageResponse<T> empty(int page, int size) {
		return new PageResponse<>(List.of(), page, size, 0, 0);
	}
}
