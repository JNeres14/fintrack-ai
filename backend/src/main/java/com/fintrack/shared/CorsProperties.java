package com.fintrack.shared;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origens permitidas (CORS_ALLOWED_ORIGINS, separadas por vírgula). Vazio = nenhuma origem; "*" é recusado (ADR-014). */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of()
				: allowedOrigins.stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
		if (allowedOrigins.stream().anyMatch(o -> o.contains("*"))) {
			throw new IllegalStateException("CORS_ALLOWED_ORIGINS não pode conter curinga (*): liste as origens.");
		}
	}
}
