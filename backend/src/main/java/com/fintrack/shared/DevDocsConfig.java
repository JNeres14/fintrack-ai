package com.fintrack.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Documentação da API, SÓ no perfil dev (ADR-019). Fora do dev esta classe nem é carregada, o springdoc fica
 * desligado e as rotas /v3/api-docs e /swagger-ui respondem 401 como qualquer outra rota protegida.
 */
@Configuration
@Profile("dev")
class DevDocsConfig {

	/** Cadeia própria, só para as rotas de documentação (somente leitura); o resto segue na cadeia principal. */
	@Bean
	@Order(1)
	SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET).permitAll().anyRequest().denyAll())
				.cors(Customizer.withDefaults());
		return http.build();
	}

	/** Botão "Authorize" do Swagger: cole o accessToken do login (sem o prefixo "Bearer"). */
	@Bean
	OpenAPI fintrackOpenApi() {
		return new OpenAPI()
				.info(new Info().title("FinTrack AI API").description("API financeira (perfil dev).").version("fase-2"))
				.components(new Components().addSecuritySchemes("bearer",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearer"));
	}
}
