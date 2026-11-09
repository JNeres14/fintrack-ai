package com.fintrack.auth;

import static com.fintrack.auth.AuthTestSupport.PASSWORD;
import static com.fintrack.auth.AuthTestSupport.login;
import static com.fintrack.auth.AuthTestSupport.refresh;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Limite baixo (3 por minuto) só nesta classe; ela tem contexto próprio, então não afeta os outros testes. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.rate-limit-per-minute=3")
class AuthRateLimitTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void loginAcimaDoLimite_devolve429ComRetryAfter() throws Exception {
		for (int i = 0; i < 3; i++) {
			login(mvc, "ninguem@teste.com", PASSWORD).andExpect(status().isUnauthorized());
		}
		login(mvc, "ninguem@teste.com", PASSWORD)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

	@Test
	void limiteEhPorEndpoint_refreshTemContadorProprio() throws Exception {
		for (int i = 0; i < 3; i++) {
			refresh(mvc, "qualquer").andExpect(status().isUnauthorized());
		}
		refresh(mvc, "qualquer").andExpect(status().isTooManyRequests());
	}
}
