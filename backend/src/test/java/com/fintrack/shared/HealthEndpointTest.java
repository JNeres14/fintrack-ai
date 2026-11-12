package com.fintrack.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** /actuator/health é público (sem token) e só diz se está de pé; nenhum outro endpoint do Actuator é exposto. */
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void healthResponde200SemAutenticacao_eNaoExpoeDetalhes() throws Exception {
		String body = mvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP")).andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("components").doesNotContain("details").doesNotContain("jdbc:")
				.doesNotContain("PostgreSQL").doesNotContain("diskSpace");
	}

	@Test
	void outrosEndpointsDoActuatorNaoSaoPublicos() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
	}
}
