package com.fintrack.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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

@SpringBootTest
@AutoConfigureMockMvc
class BudgetApiTest {

	private static final String BASE = "/api/budgets";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private TestDatabaseCleaner cleaner;
	@MockitoBean
	private AppToday today;

	private Session ana;
	private Session beto;
	private UUID alimentacao;
	private UUID mercado;
	private UUID lazer;
	private UUID salario;

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		when(today.today()).thenReturn(LocalDate.of(2026, 11, 20));
		ana = TestUsers.create(mvc, "ana@teste.com");
		beto = TestUsers.create(mvc, "beto@teste.com");
		alimentacao = category(ana, "Alimentação");
		mercado = category(ana, "Mercado");
		lazer = category(ana, "Lazer");
		salario = category(ana, "Salário");
	}

	private UUID category(Session s, String name) {
		return jdbc.queryForObject("select id from categories where user_id = ? and name = ? limit 1", UUID.class,
				s.userId(), name);
	}

	private ResultActions create(Session s, String month, UUID category, String limit) throws Exception {
		String body = "{" + (month == null ? "" : "\"month\":\"" + month + "\",")
				+ (category == null ? "" : "\"categoryId\":\"" + category + "\",") + "\"limit\":" + limit + "}";
		return mvc.perform(post(BASE).header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private UUID idOf(ResultActions r) throws Exception {
		return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private void expense(Session s, UUID category, String type, String amount, String date) {
		UUID account = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, s.userId());
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on) "
				+ "values (?, ?, ?, ?, ?, ?)", s.userId(), account, category, type, new BigDecimal(amount),
				LocalDate.parse(date));
	}

	private void seedConsumo() {
		expense(ana, mercado, "EXPENSE", "50.00", "2026-11-30"); // último dia do mês: conta em novembro
		expense(ana, mercado, "EXPENSE", "25.50", "2026-12-01"); // primeiro dia do mês seguinte: conta em dezembro
		expense(ana, mercado, "EXPENSE", "100.00", "2026-10-31"); // mês anterior: não conta
		expense(ana, alimentacao, "EXPENSE", "20.00", "2026-11-01");
		expense(ana, lazer, "EXPENSE", "80.00", "2026-11-15");
		expense(ana, salario, "INCOME", "3000.00", "2026-11-10"); // receita não é consumo
		expense(beto, category(beto, "Mercado"), "EXPENSE", "999.00", "2026-11-10"); // outro usuário
	}

	// ---------- CRUD e validação ----------

	@Test
	void criaOrcamentoGeralEPorCategoria_eListaComGeralPrimeiro() throws Exception {
		create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoryName").value("Lazer"))
				.andExpect(jsonPath("$.month").value("2026-11"))
				.andExpect(jsonPath("$.limit").value(100.00))
				.andExpect(jsonPath("$.spent").value(0.0));
		create(ana, "2026-11", null, "500.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoryId").doesNotExist());
		create(ana, "2026-11", alimentacao, "200.00").andExpect(status().isCreated());

		String body = mvc.perform(get(BASE + "?month=2026-11").header("Authorization", ana.bearer()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<String>>read(body, "$[*].categoryName")).containsExactly(null, "Alimentação", "Lazer");
	}

	@Test
	void listaSemMes_usaOMesDeHoje() throws Exception {
		create(ana, "2026-11", null, "500.00").andExpect(status().isCreated());
		create(ana, "2026-12", null, "300.00").andExpect(status().isCreated());

		mvc.perform(get(BASE).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].month").value("2026-11"));
	}

	@Test
	void validacoes_devolve400_ou404() throws Exception {
		create(ana, "2026-11", null, "0").andExpect(status().isBadRequest());
		create(ana, "2026-11", null, "-10.00").andExpect(status().isBadRequest());
		create(ana, "2026-11", null, "10.123").andExpect(status().isBadRequest());
		create(ana, null, null, "10.00").andExpect(status().isBadRequest()); // sem mês
		create(ana, "2026-13", null, "10.00").andExpect(status().isBadRequest());
		create(ana, "novembro", null, "10.00").andExpect(status().isBadRequest());
		create(ana, "1999-12", null, "10.00").andExpect(status().isBadRequest());
		create(ana, "2099-01", null, "10.00").andExpect(status().isBadRequest());
		create(ana, "2026-11", salario, "10.00").andExpect(status().isBadRequest()); // categoria de receita
		create(ana, "2026-11", UUID.randomUUID(), "10.00").andExpect(status().isNotFound());
		mvc.perform(get(BASE + "?month=2026-13").header("Authorization", ana.bearer()))
				.andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("select count(*) from budgets", Integer.class)).isZero();
	}

	@Test
	void duplicado_devolve409_mesComOutraCategoriaOuOutroMesPodem() throws Exception {
		create(ana, "2026-11", null, "500.00").andExpect(status().isCreated());
		create(ana, "2026-11", null, "600.00").andExpect(status().isConflict());
		create(ana, "2026-12", null, "600.00").andExpect(status().isCreated());

		create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated()); // geral e categoria coexistem
		create(ana, "2026-11", lazer, "120.00").andExpect(status().isConflict());
		create(ana, "2026-12", lazer, "100.00").andExpect(status().isCreated());
		create(ana, "2026-11", alimentacao, "100.00").andExpect(status().isCreated());
		create(ana, "2026-11", mercado, "100.00").andExpect(status().isCreated()); // pai e filho coexistem
	}

	@Test
	void editaSoOLimite_eExclui() throws Exception {
		UUID id = idOf(create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated()));

		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"limit\":250.50,\"month\":\"2027-01\",\"categoryId\":\"" + mercado + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.limit").value(250.50))
				.andExpect(jsonPath("$.month").value("2026-11")) // mês e categoria não mudam
				.andExpect(jsonPath("$.categoryId").value(lazer.toString()));
		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"limit\":0}")).andExpect(status().isBadRequest());

		mvc.perform(delete(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNotFound());
	}

	@Test
	void categoriaComOrcamentoNaoPodeSerExcluida() throws Exception {
		create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated());

		mvc.perform(delete("/api/categories/" + lazer).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict());
	}

	// ---------- consumo ----------

	@Test
	void consumo_respeitaOsLimitesDoMes_soContaDespesa_eNaoMisturaUsuarios() throws Exception {
		seedConsumo();
		create(ana, "2026-11", null, "500.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(150.00)) // 50.00 + 20.00 + 80.00
				.andExpect(jsonPath("$.remaining").value(350.00))
				.andExpect(jsonPath("$.percentUsed").value(30.0))
				.andExpect(jsonPath("$.status").value("OK"));
		create(ana, "2026-12", null, "500.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(25.50)); // só a compra de 01/12
	}

	@Test
	void categoriaPaiIncluiAsSubcategorias_eFilhaContaSoAsSuas() throws Exception {
		seedConsumo();

		create(ana, "2026-11", alimentacao, "200.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(70.00)) // 20.00 do pai + 50.00 de Mercado (filha)
				.andExpect(jsonPath("$.percentUsed").value(35.0));
		create(ana, "2026-11", mercado, "100.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(50.00))
				.andExpect(jsonPath("$.percentUsed").value(50.0))
				.andExpect(jsonPath("$.status").value("OK"));
	}

	@Test
	void status_okWarningExceeded_comValoresExatos() throws Exception {
		seedConsumo(); // Lazer em novembro: 80.00

		create(ana, "2026-11", lazer, "100.01").andExpect(status().isCreated())
				.andExpect(jsonPath("$.percentUsed").value(80.0)) // arredondado para 80,0 ...
				.andExpect(jsonPath("$.status").value("OK")); // ... mas o valor exato é 79,99%: ainda OK
		UUID id = jdbc.queryForObject("select id from budgets", UUID.class);
		String put = "{\"limit\":%s}";

		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(put.formatted("100.00"))).andExpect(jsonPath("$.status").value("WARNING"))
				.andExpect(jsonPath("$.percentUsed").value(80.0));
		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(put.formatted("80.00"))).andExpect(jsonPath("$.status").value("WARNING")) // gasto = limite
				.andExpect(jsonPath("$.remaining").value(0.0)).andExpect(jsonPath("$.percentUsed").value(100.0));
		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(put.formatted("79.99"))).andExpect(jsonPath("$.status").value("EXCEEDED"))
				.andExpect(jsonPath("$.remaining").value(-0.01)).andExpect(jsonPath("$.percentUsed").value(100.0));
	}

	@Test
	void consumoAcompanhaAsTransacoesCriadasPelaApi() throws Exception {
		UUID id = idOf(create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated()));
		UUID account = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
		String tx = "{\"accountId\":\"" + account + "\",\"categoryId\":\"" + lazer
				+ "\",\"type\":\"EXPENSE\",\"amount\":33.33,\"occurredOn\":\"2026-11-12\"}";
		mvc.perform(post("/api/transactions").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(tx)).andExpect(status().isCreated());

		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.spent").value(33.33)).andExpect(jsonPath("$.remaining").value(66.67))
				.andExpect(jsonPath("$.percentUsed").value(33.3));
	}

	// ---------- isolamento entre usuários ----------

	@Test
	void acessoCruzado_usuarioBNaoLeAlteraNemApagaOrcamentoDeA_resposta404() throws Exception {
		UUID id = idOf(create(ana, "2026-11", lazer, "100.00").andExpect(status().isCreated()));

		mvc.perform(get(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(put(BASE + "/" + id).header("Authorization", beto.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"limit\":1.00}")).andExpect(status().isNotFound());
		mvc.perform(delete(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(get(BASE + "?month=2026-11").header("Authorization", beto.bearer()))
				.andExpect(jsonPath("$.length()").value(0));

		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.limit").value(100.00)); // intacto
	}

	@Test
	void usuarioBNaoCriaOrcamentoComCategoriaDeA_eSeuConsumoNaoVeGastosDeA() throws Exception {
		seedConsumo();

		create(beto, "2026-11", lazer, "100.00").andExpect(status().isNotFound()); // categoria do Ana
		// O orçamento geral do Beto soma só as despesas do Beto (999.00), nunca as do Ana (150.00)
		create(beto, "2026-11", null, "2000.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(999.00));
		// O mesmo mês e categoria em usuários diferentes não conflitam
		create(ana, "2026-11", null, "500.00").andExpect(status().isCreated());
	}

	@Test
	void semToken_devolve401() throws Exception {
		mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mvc.perform(get(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(put(BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}
}
