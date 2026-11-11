package com.fintrack.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.support.TestUsers;
import com.fintrack.support.TestUsers.Session;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** OpenAPI só existe no perfil dev (ADR-019). */
class OpenApiProfileTest {

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@ActiveProfiles({ "test", "dev" })
	class NoPerfilDev {

		@Autowired
		private MockMvc mvc;

		@Test
		void apiDocsEmSwaggerUi_ficamAbertosParaLeitura_eListamAsRotas() throws Exception {
			String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
					.getContentAsString();

			assertThat(json).contains("/api/transactions", "/api/budgets", "/api/cards", "/api/auth/login");
			assertThat(json).contains("bearer"); // esquema de segurança para o botão Authorize
			mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		}

		@Test
		void abrirADocumentacaoNaoAbreAApi_eEscritaNasRotasDeDocsEhNegada() throws Exception {
			mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
			mvc.perform(post("/v3/api-docs").contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().is4xxClientError());
		}
	}

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	class ForaDoPerfilDev {

		@Autowired
		private MockMvc mvc;

		@Test
		void semToken_documentacaoDevolve401() throws Exception {
			mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
			mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
			mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
		}

		@Test
		void comToken_documentacaoNaoExiste_devolve404() throws Exception {
			Session s = TestUsers.create(mvc, "docs@teste.com");

			mvc.perform(get("/v3/api-docs").header("Authorization", s.bearer())).andExpect(status().isNotFound());
			mvc.perform(get("/swagger-ui/index.html").header("Authorization", s.bearer()))
					.andExpect(status().isNotFound());
		}
	}
}
