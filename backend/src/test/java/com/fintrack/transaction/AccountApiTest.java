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
class AccountApiTest {

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

	private ResultActions createAccount(Session s, String name, String type, String initial) throws Exception {
		String body = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\""
				+ (initial == null ? "" : ",\"initialBalance\":" + initial) + "}";
		return mvc.perform(post("/api/accounts").header("Authorization", s.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String idOf(ResultActions r) throws Exception {
		return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private void addTransaction(Session s, String accountId, String type, String amount) {
		UUID category = jdbc.queryForObject("select id from categories where user_id = ? and name = 'Mercado'",
				UUID.class, s.userId());
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on) "
				+ "values (?, ?, ?, ?, ?, ?)", s.userId(), UUID.fromString(accountId), category, type,
				new BigDecimal(amount), LocalDate.of(2026, 11, 1));
	}

	@Test
	void novoUsuarioJaTemContaCarteira_comSaldoZero() throws Exception {
		mvc.perform(get("/api/accounts").header("Authorization", ana.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Carteira"))
				.andExpect(jsonPath("$[0].type").value("WALLET"))
				.andExpect(jsonPath("$[0].balance").value(0.0));
	}

	@Test
	void crudCompleto() throws Exception {
		ResultActions created = createAccount(ana, "Nubank", "CHECKING", "1500.50")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.initialBalance").value(1500.50))
				.andExpect(jsonPath("$.balance").value(1500.50));
		String id = idOf(created);

		mvc.perform(get("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nubank"));

		mvc.perform(put("/api/accounts/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nubank PJ\",\"type\":\"SAVINGS\",\"initialBalance\":10.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Nubank PJ"))
				.andExpect(jsonPath("$.type").value("SAVINGS"))
				.andExpect(jsonPath("$.balance").value(10.0));

		mvc.perform(delete("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isNotFound());
	}

	@Test
	void saldoEhCalculado_inicialMaisReceitasMenosDespesas_semErroDeCentavos() throws Exception {
		String id = idOf(createAccount(ana, "Banco", "CHECKING", "50.00"));
		String outra = idOf(createAccount(ana, "Outra", "WALLET", null));
		addTransaction(ana, id, "INCOME", "100.10");
		addTransaction(ana, id, "EXPENSE", "30.05");
		addTransaction(ana, id, "EXPENSE", "0.01");
		addTransaction(ana, outra, "EXPENSE", "5.00");

		// 50.00 + 100.10 - 30.05 - 0.01 = 120.04
		mvc.perform(get("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.balance").value(120.04))
				.andExpect(jsonPath("$.initialBalance").value(50.0));
		String list = mvc.perform(get("/api/accounts").header("Authorization", ana.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<java.util.List<Double>>read(list, "$[?(@.name=='Banco')].balance")).containsExactly(120.04);
		assertThat(JsonPath.<java.util.List<Double>>read(list, "$[?(@.name=='Outra')].balance")).containsExactly(-5.0);
		assertThat(JsonPath.<java.util.List<Double>>read(list, "$[?(@.name=='Carteira')].balance")).containsExactly(0.0);
	}

	@Test
	void saldoDeUmUsuarioNaoVazaParaOutro() throws Exception {
		String id = idOf(createAccount(ana, "Banco", "CHECKING", "50.00"));
		addTransaction(ana, id, "INCOME", "999.99");

		createAccount(beto, "Banco", "CHECKING", "50.00").andExpect(status().isCreated()); // mesmo nome: ok
		String list = mvc.perform(get("/api/accounts").header("Authorization", beto.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<java.util.List<Double>>read(list, "$[?(@.name=='Banco')].balance")).containsExactly(50.0);
	}

	@Test
	void validacoes_devolve400() throws Exception {
		createAccount(ana, " ", "WALLET", null).andExpect(status().isBadRequest());
		createAccount(ana, "X", "INEXISTENTE", null).andExpect(status().isBadRequest());
		createAccount(ana, "X", "WALLET", "10.123").andExpect(status().isBadRequest()); // 3 casas decimais
		mvc.perform(post("/api/accounts").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
				.andExpect(status().isBadRequest()); // sem tipo
	}

	@Test
	void nomeRepetido_semDiferenciarMaiusculas_devolve409() throws Exception {
		createAccount(ana, "Banco", "CHECKING", null).andExpect(status().isCreated());
		createAccount(ana, "BANCO", "CHECKING", null).andExpect(status().isConflict());
		createAccount(ana, "carteira", "WALLET", null).andExpect(status().isConflict()); // a padrão

		String id = idOf(createAccount(ana, "Poupança", "SAVINGS", null));
		mvc.perform(put("/api/accounts/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"banco\",\"type\":\"SAVINGS\"}"))
				.andExpect(status().isConflict());
		mvc.perform(put("/api/accounts/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"POUPANÇA\",\"type\":\"SAVINGS\"}"))
				.andExpect(status().isOk()); // trocar a própria caixa é permitido
	}

	@Test
	void excluirContaComTransacoes_devolve409_esemTransacoesFunciona() throws Exception {
		String id = idOf(createAccount(ana, "Banco", "CHECKING", null));
		addTransaction(ana, id, "EXPENSE", "10.00");

		mvc.perform(delete("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict());
		jdbc.update("delete from transactions");
		mvc.perform(delete("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isNoContent());
	}

	@Test
	void acessoCruzado_usuarioBNaoLeAlteraNemApagaContaDeA_resposta404() throws Exception {
		String id = idOf(createAccount(ana, "Segredo", "CHECKING", "100.00"));

		mvc.perform(get("/api/accounts/" + id).header("Authorization", beto.bearer()))
				.andExpect(status().isNotFound());
		mvc.perform(put("/api/accounts/" + id).header("Authorization", beto.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invadida\",\"type\":\"OTHER\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(delete("/api/accounts/" + id).header("Authorization", beto.bearer()))
				.andExpect(status().isNotFound());

		String listaDoBeto = mvc.perform(get("/api/accounts").header("Authorization", beto.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(listaDoBeto).doesNotContain(id).doesNotContain("Segredo");
		// A conta de A segue intacta
		mvc.perform(get("/api/accounts/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.name").value("Segredo"));
	}

	@Test
	void userIdNoCorpoEhIgnorado_contaFicaComODonoDoToken() throws Exception {
		String body = "{\"name\":\"Tentativa\",\"type\":\"WALLET\",\"userId\":\"" + beto.userId() + "\"}";
		String id = idOf(mvc.perform(post("/api/accounts").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()));

		assertThat(jdbc.queryForObject("select user_id from accounts where id = ?", UUID.class, UUID.fromString(id)))
				.isEqualTo(ana.userId());
	}

	@Test
	void semToken_todosOsEndpointsDevolvem401() throws Exception {
		for (String base : new String[] { "/api/accounts", "/api/categories", "/api/payment-methods" }) {
			mvc.perform(get(base)).andExpect(status().isUnauthorized());
			mvc.perform(get(base + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
			mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isUnauthorized());
			mvc.perform(put(base + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isUnauthorized());
			mvc.perform(delete(base + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		}
	}
}
