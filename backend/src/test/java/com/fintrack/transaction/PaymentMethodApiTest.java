package com.fintrack.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fintrack.support.TestDatabaseConfig.TestDatabaseCleaner;
import com.fintrack.support.TestUsers;
import com.fintrack.support.TestUsers.Session;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodApiTest {

	private static final String BASE = "/api/payment-methods";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private TestDatabaseCleaner cleaner;

	private Session ana;
	private Session beto;

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		ana = TestUsers.create(mvc, "ana@teste.com");
		beto = TestUsers.create(mvc, "beto@teste.com");
	}

	private ResultActions create(Session s, String name) throws Exception {
		return mvc.perform(post(BASE).header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\"}"));
	}

	private String idOf(ResultActions r) throws Exception {
		return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void novoUsuarioTemPixDebitoCreditoEDinheiro() throws Exception {
		String list = mvc.perform(get(BASE).header("Authorization", ana.bearer()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<String>>read(list, "$[*].name")).containsExactlyInAnyOrder("Pix", "Débito",
				"Crédito", "Dinheiro");
	}

	@Test
	void crudCompleto() throws Exception {
		String id = idOf(create(ana, "Vale-refeição").andExpect(status().isCreated()));
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.name").value("Vale-refeição"));
		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"VR\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("VR"));
		mvc.perform(delete(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNotFound());
	}

	@Test
	void nomeEmBranco_devolve400_eRepetido_devolve409() throws Exception {
		create(ana, "  ").andExpect(status().isBadRequest());
		create(ana, "pix").andExpect(status().isConflict()); // o padrão "Pix", sem diferenciar caixa
	}

	@Test
	void excluirEmUso_devolve409() throws Exception {
		UUID pix = jdbc.queryForObject("select id from payment_methods where user_id = ? and name = 'Pix'",
				UUID.class, ana.userId());
		UUID account = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
		UUID category = jdbc.queryForObject("select id from categories where user_id = ? and name = 'Mercado'",
				UUID.class, ana.userId());
		jdbc.update("insert into transactions (user_id, account_id, category_id, payment_method_id, type, amount, "
				+ "occurred_on) values (?, ?, ?, ?, 'EXPENSE', ?, ?)", ana.userId(), account, category, pix,
				new BigDecimal("9.90"), LocalDate.of(2026, 11, 1));

		mvc.perform(delete(BASE + "/" + pix).header("Authorization", ana.bearer())).andExpect(status().isConflict());
		jdbc.update("delete from transactions");
		mvc.perform(delete(BASE + "/" + pix).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
	}

	@Test
	void acessoCruzado_usuarioBNaoLeAlteraNemApaga_resposta404() throws Exception {
		String id = idOf(create(ana, "Cartão secreto").andExpect(status().isCreated()));

		mvc.perform(get(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(put(BASE + "/" + id).header("Authorization", beto.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Invadido\"}")).andExpect(status().isNotFound());
		mvc.perform(delete(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());

		String lista = mvc.perform(get(BASE).header("Authorization", beto.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(lista).doesNotContain(id).doesNotContain("secreto");
		create(beto, "Cartão secreto").andExpect(status().isCreated()); // mesmo nome em outro usuário é permitido
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.name").value("Cartão secreto"));
	}
}
