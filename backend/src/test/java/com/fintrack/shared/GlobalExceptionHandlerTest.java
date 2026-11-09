package com.fintrack.shared;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Sem contexto Spring: controller de teste + o handler real, para provar o formato padronizado dos erros. */
class GlobalExceptionHandlerTest {

	record SampleRequest(@NotBlank String description, @Money @Positive BigDecimal amount) {
	}

	record SampleResponse(BigDecimal amount) {
	}

	@RestController
	static class SampleController {

		@PostMapping("/sample")
		SampleResponse create(@Valid @RequestBody SampleRequest request) {
			return new SampleResponse(request.amount());
		}

		@GetMapping("/sample/missing")
		void missing() {
			throw new ResourceNotFoundException("conta 123 de outro usuário");
		}

		@GetMapping("/sample/boom")
		void boom() {
			throw new IllegalStateException("senha=segredo valor=999.99 estabelecimento=Padaria");
		}

		@GetMapping("/sample/money")
		SampleResponse money() {
			return new SampleResponse(new BigDecimal("10.50"));
		}
	}

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new SampleController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void validacaoRetornaCamposSemOValorRejeitado() throws Exception {
		mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"\",\"amount\":-1}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.errors[?(@.field=='description')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field=='amount')]").exists())
				.andExpect(content().string(not(containsString("-1"))));
	}

	@Test
	void recusaMaisDeDuasCasasDecimais() throws Exception {
		mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"x\",\"amount\":10.505}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("amount"));
	}

	@Test
	void aceitaDuasCasasDecimais() throws Exception {
		mvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"x\",\"amount\":10.55}"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"amount\":10.55}"));
	}

	@Test
	void dinheiroSaiComoNumeroPreservandoAsCasas() throws Exception {
		mvc.perform(get("/sample/money"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"amount\":10.50")));
	}

	@Test
	void naoEncontradoRetorna404SemDetalhes() throws Exception {
		mvc.perform(get("/sample/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Não encontrado"))
				.andExpect(content().string(not(containsString("123"))));
	}

	@Test
	void erroInesperadoRetorna500GenericoSemVazarDados() throws Exception {
		mvc.perform(get("/sample/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(not(containsString("segredo"))))
				.andExpect(content().string(not(containsString("999.99"))))
				.andExpect(content().string(not(containsString("Padaria"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))))
				.andExpect(content().string(not(containsString("at com.fintrack"))));
	}
}
