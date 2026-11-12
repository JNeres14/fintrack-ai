package com.fintrack.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.shared.AppToday;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Isolamento entre usuários (fechamento da Fase 2): o usuário B recebe 404 (não 403, para não revelar que o
 * recurso existe) ao ler, editar ou apagar conta, cartão, transação e orçamento do usuário A, e nada muda.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossUserIsolationTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private TestDatabaseCleaner cleaner;
	@MockitoBean
	private AppToday today;

	private Session a;
	private Session b;
	private UUID conta;
	private UUID cartao;
	private UUID transacao;
	private UUID orcamento;
	private UUID categoria;

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		when(today.today()).thenReturn(LocalDate.of(2026, 11, 20));
		a = TestUsers.create(mvc, "a@teste.com");
		b = TestUsers.create(mvc, "b@teste.com");
		categoria = jdbc.queryForObject("select id from categories where user_id = ? and name = 'Mercado'",
				UUID.class, a.userId());
		conta = idOf(send(post("/api/accounts"), a, "{\"name\":\"Conta A\",\"type\":\"CHECKING\"}"));
		cartao = idOf(send(post("/api/accounts"), a,
				"{\"name\":\"Cartão A\",\"type\":\"CREDIT_CARD\",\"creditLimit\":1000.00,\"closingDay\":25,\"dueDay\":5}"));
		transacao = idOf(send(post("/api/transactions"), a, txJson("10.00")));
		orcamento = idOf(send(post("/api/budgets"), a,
				"{\"month\":\"2026-11\",\"categoryId\":\"" + categoria + "\",\"limit\":100.00}"));
	}

	private String txJson(String amount) {
		return "{\"accountId\":\"" + conta + "\",\"categoryId\":\"" + categoria
				+ "\",\"type\":\"EXPENSE\",\"amount\":" + amount + ",\"occurredOn\":\"2026-11-10\"}";
	}

	private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
			Session s, String body) throws Exception {
		return mvc.perform(req.header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(body)).andExpect(status().isCreated());
	}

	private UUID idOf(ResultActions r) throws Exception {
		return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private void bCannotReadEditOrDelete(String url, String putBody) throws Exception {
		mvc.perform(get(url).header("Authorization", b.bearer())).andExpect(status().isNotFound());
		mvc.perform(put(url).header("Authorization", b.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(putBody)).andExpect(status().isNotFound());
		mvc.perform(delete(url).header("Authorization", b.bearer())).andExpect(status().isNotFound());
	}

	@Test
	void conta_BNaoLeEditaNemApaga() throws Exception {
		bCannotReadEditOrDelete("/api/accounts/" + conta, "{\"name\":\"Invadida\",\"type\":\"CHECKING\"}");
		mvc.perform(get("/api/accounts/" + conta).header("Authorization", a.bearer()))
				.andExpect(jsonPath("$.name").value("Conta A"));
	}

	@Test
	void cartao_BNaoLeEditaNemApaga() throws Exception {
		bCannotReadEditOrDelete("/api/accounts/" + cartao,
				"{\"name\":\"Invadido\",\"type\":\"CREDIT_CARD\",\"creditLimit\":1.00,\"closingDay\":1,\"dueDay\":2}");
		mvc.perform(get("/api/accounts/" + cartao).header("Authorization", a.bearer()))
				.andExpect(jsonPath("$.name").value("Cartão A")).andExpect(jsonPath("$.creditLimit").value(1000.00));
		mvc.perform(get("/api/cards").header("Authorization", b.bearer())).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void transacao_BNaoLeEditaNemApaga() throws Exception {
		bCannotReadEditOrDelete("/api/transactions/" + transacao, txJson("1.00"));
		mvc.perform(get("/api/transactions/" + transacao).header("Authorization", a.bearer()))
				.andExpect(jsonPath("$.amount").value(10.00));
	}

	@Test
	void orcamento_BNaoLeEditaNemApaga() throws Exception {
		bCannotReadEditOrDelete("/api/budgets/" + orcamento, "{\"limit\":1.00}");
		mvc.perform(get("/api/budgets/" + orcamento).header("Authorization", a.bearer()))
				.andExpect(jsonPath("$.limit").value(100.00));
	}

	@Test
	void nada_mudouNoBancoDepoisDasTentativasDeB() throws Exception {
		conta_BNaoLeEditaNemApaga();
		cartao_BNaoLeEditaNemApaga();
		transacao_BNaoLeEditaNemApaga();
		orcamento_BNaoLeEditaNemApaga();

		assertThat(jdbc.queryForObject("select count(*) from accounts where user_id = ? and name in ('Conta A','Cartão A')",
				Integer.class, a.userId())).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from transactions where user_id = ?", Integer.class,
				a.userId())).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from budgets where user_id = ?", Integer.class, a.userId()))
				.isEqualTo(1);
	}
}
