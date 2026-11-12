package com.fintrack.budget;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

/** Testes de fechamento do Bloco E: cenário de ponta a ponta do orçamento mensal (limite 600, mês 2026-11). */
@SpringBootTest
@AutoConfigureMockMvc
class BudgetClosingTest {

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
	private UUID lazer;
	private UUID salario;
	private UUID conta;

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		when(today.today()).thenReturn(LocalDate.of(2026, 11, 20));
		ana = TestUsers.create(mvc, "ana@teste.com");
		lazer = category("Lazer");
		salario = category("Salário");
		conta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
	}

	private UUID category(String name) {
		return jdbc.queryForObject("select id from categories where user_id = ? and name = ? limit 1", UUID.class,
				ana.userId(), name);
	}

	private void seed(UUID account, UUID category, String type, String amount, String date) {
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on) "
				+ "values (?, ?, ?, ?, ?, ?)", ana.userId(), account, category, type, new BigDecimal(amount),
				LocalDate.parse(date));
	}

	private ResultActions create(String month, UUID category, String limit) throws Exception {
		String body = "{\"month\":\"" + month + "\","
				+ (category == null ? "" : "\"categoryId\":\"" + category + "\",") + "\"limit\":" + limit + "}";
		return mvc.perform(post(BASE).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions changeLimit(UUID id, String limit) throws Exception {
		return mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"limit\":" + limit + "}"));
	}

	private UUID idOf(ResultActions r) throws Exception {
		return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	@Test
	void limite600Com83PorCento_warning_duplicadoDa409_eReduzirParaTrezentosEstoura() throws Exception {
		seed(conta, lazer, "EXPENSE", "300.00", "2026-11-03");
		seed(conta, lazer, "EXPENSE", "199.50", "2026-11-18"); // total 499.50 = 83,25% de 600

		UUID id = idOf(create("2026-11", lazer, "600.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(499.50)).andExpect(jsonPath("$.remaining").value(100.50))
				.andExpect(jsonPath("$.percentUsed").value(83.3)).andExpect(jsonPath("$.status").value("WARNING")));

		create("2026-11", lazer, "600.00").andExpect(status().isConflict()); // mesmo mês e categoria

		changeLimit(id, "400.00").andExpect(status().isOk()).andExpect(jsonPath("$.spent").value(499.50))
				.andExpect(jsonPath("$.remaining").value(-99.50)).andExpect(jsonPath("$.status").value("EXCEEDED"));
	}

	@Test
	void orcamentoGeralSemCategoria_somaTodasAsDespesasDoMes_eIgnoraReceita() throws Exception {
		UUID mercado = category("Mercado");
		seed(conta, lazer, "EXPENSE", "100.00", "2026-11-05");
		seed(conta, mercado, "EXPENSE", "200.00", "2026-11-06");
		seed(conta, salario, "INCOME", "5000.00", "2026-11-05");

		create("2026-11", null, "1000.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoryId").doesNotExist()).andExpect(jsonPath("$.spent").value(300.00))
				.andExpect(jsonPath("$.percentUsed").value(30.0)).andExpect(jsonPath("$.status").value("OK"));
	}

	@Test
	void bordas_abaixoDeOitentaEhOk_exatamenteOitentaEhWarning_categoriaDeReceitaDa400() throws Exception {
		seed(conta, lazer, "EXPENSE", "479.40", "2026-11-05"); // 79,9% de 600
		UUID id = idOf(create("2026-11", lazer, "600.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OK")));

		seed(conta, lazer, "EXPENSE", "0.60", "2026-11-06"); // 480.00 = exatamente 80% de 600
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.spent").value(480.00)).andExpect(jsonPath("$.percentUsed").value(80.0))
				.andExpect(jsonPath("$.status").value("WARNING"));

		create("2026-11", salario, "100.00").andExpect(status().isBadRequest()); // categoria de receita
	}

	@Test
	void pagamentoDeFaturaNaoEntraNoConsumoDoOrcamento() throws Exception {
		when(today.today()).thenReturn(LocalDate.of(2026, 12, 26)); // fatura de dezembro (fecha dia 25) já fechada
		String cartaoJson = "{\"name\":\"Nubank\",\"type\":\"CREDIT_CARD\",\"creditLimit\":5000.00,\"closingDay\":25,\"dueDay\":5}";
		UUID cartao = idOf(mvc.perform(post("/api/accounts").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(cartaoJson)).andExpect(status().isCreated()));
		seed(cartao, lazer, "EXPENSE", "90.00", "2026-12-05"); // a compra no cartão é gasto

		UUID id = idOf(create("2026-12", null, "1000.00").andExpect(status().isCreated())
				.andExpect(jsonPath("$.spent").value(90.00)));

		String pagamento = "{\"sourceAccountId\":\"" + conta + "\",\"amount\":90.00,\"paidOn\":\"2026-12-26\"}";
		mvc.perform(post("/api/accounts/" + cartao + "/invoices/2026-12/payments")
				.header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON).content(pagamento))
				.andExpect(status().isCreated());

		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.spent").value(90.00)) // continua 90, não 180
				.andExpect(jsonPath("$.remaining").value(910.00));
	}
}
