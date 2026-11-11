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
import java.util.ArrayList;
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
class TransactionApiTest {

	private static final String BASE = "/api/transactions";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private TestDatabaseCleaner cleaner;

	private Session ana;
	private Session beto;
	private UUID anaConta;
	private UUID anaMercado;
	private UUID anaAlimentacao;
	private UUID anaSalario;
	private UUID anaLazer;
	private UUID anaPix;

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		ana = TestUsers.create(mvc, "ana@teste.com");
		beto = TestUsers.create(mvc, "beto@teste.com");
		anaConta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
		anaMercado = category(ana, "Mercado");
		anaAlimentacao = category(ana, "Alimentação");
		anaSalario = category(ana, "Salário");
		anaLazer = category(ana, "Lazer");
		anaPix = jdbc.queryForObject("select id from payment_methods where user_id = ? and name = 'Pix'", UUID.class,
				ana.userId());
	}

	private UUID category(Session s, String name) {
		return jdbc.queryForObject("select id from categories where user_id = ? and name = ? limit 1", UUID.class,
				s.userId(), name);
	}

	/** amount entra como texto cru do JSON, para testar a precisão sem passar por double. */
	private static String json(UUID account, UUID category, UUID payment, String type, String amount, String date,
			String merchant, String description) {
		return "{\"accountId\":\"" + account + "\",\"categoryId\":\"" + category + "\""
				+ (payment == null ? "" : ",\"paymentMethodId\":\"" + payment + "\"")
				+ ",\"type\":\"" + type + "\",\"amount\":" + amount + ",\"occurredOn\":\"" + date + "\""
				+ (merchant == null ? "" : ",\"merchant\":\"" + merchant + "\"")
				+ (description == null ? "" : ",\"description\":\"" + description + "\"") + "}";
	}

	private ResultActions create(Session s, String body) throws Exception {
		return mvc.perform(post(BASE).header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions createExpense(String amount, String date) throws Exception {
		return create(ana, json(anaConta, anaMercado, null, "EXPENSE", amount, date, null, null));
	}

	private String idOf(ResultActions r) throws Exception {
		return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private void seed(UUID category, String type, String amount, String date, String merchant, String description) {
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on, merchant, "
				+ "description) values (?, ?, ?, ?, ?, ?, ?, ?)", ana.userId(), anaConta, category, type,
				new BigDecimal(amount), LocalDate.parse(date), merchant, description);
	}

	private String list(Session s, String query) throws Exception {
		return mvc.perform(get(BASE + query).header("Authorization", s.bearer())).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private int total(Session s, String query) throws Exception {
		return JsonPath.<Integer>read(list(s, query), "$.totalElements");
	}

	private int totalByText(String q) throws Exception {
		String body = mvc.perform(get(BASE).param("q", q).header("Authorization", ana.bearer()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.<Integer>read(body, "$.totalElements");
	}

	// ---------- CRUD ----------

	@Test
	void criaEBusca_comTodosOsCampos_eTextosLimpos() throws Exception {
		String id = idOf(create(ana, json(anaConta, anaMercado, anaPix, "EXPENSE", "10.10", "2026-11-03",
				"  Supermercado  ", "Compras da semana")).andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount").value(10.10))
				.andExpect(jsonPath("$.merchant").value("Supermercado"))
				.andExpect(jsonPath("$.paymentMethodId").value(anaPix.toString())));

		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("EXPENSE"))
				.andExpect(jsonPath("$.occurredOn").value("2026-11-03"))
				.andExpect(jsonPath("$.description").value("Compras da semana"));
		assertThat(jdbc.queryForObject("select amount::text from transactions", String.class)).isEqualTo("10.10");
	}

	@Test
	void formaDePagamentoEhOpcional_eTextoVazioViraNulo() throws Exception {
		create(ana, json(anaConta, anaMercado, null, "EXPENSE", "5.00", "2026-11-03", "   ", null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.paymentMethodId").doesNotExist())
				.andExpect(jsonPath("$.merchant").doesNotExist());
	}

	@Test
	void editaEExclui() throws Exception {
		String id = idOf(createExpense("10.00", "2026-11-03").andExpect(status().isCreated()));

		mvc.perform(put(BASE + "/" + id).header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(json(anaConta, anaSalario, anaPix, "INCOME", "3000.50", "2026-11-05", "Empresa", null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("INCOME"))
				.andExpect(jsonPath("$.amount").value(3000.50))
				.andExpect(jsonPath("$.categoryId").value(anaSalario.toString()));

		mvc.perform(delete(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer())).andExpect(status().isNotFound());
	}

	// ---------- precisão e validação ----------

	@Test
	void precisao_aceitaOsLimites_eRecusaMaisDeDuasCasas_semArredondarEmSilencio() throws Exception {
		createExpense("0.01", "2026-11-03").andExpect(status().isCreated());
		createExpense("999999999999.99", "2026-11-03").andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount").value(999999999999.99));
		createExpense("12.345", "2026-11-03").andExpect(status().isBadRequest());
		createExpense("0.001", "2026-11-03").andExpect(status().isBadRequest());
		createExpense("1000000000000.00", "2026-11-03").andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(2);
	}

	@Test
	void valorZeroOuNegativoOuAusente_devolve400() throws Exception {
		createExpense("0", "2026-11-03").andExpect(status().isBadRequest());
		createExpense("-5.00", "2026-11-03").andExpect(status().isBadRequest());
		create(ana, "{\"accountId\":\"" + anaConta + "\",\"categoryId\":\"" + anaMercado
				+ "\",\"type\":\"EXPENSE\",\"occurredOn\":\"2026-11-03\"}").andExpect(status().isBadRequest());
	}

	@Test
	void dataForaDaFaixaOuInvalida_devolve400() throws Exception {
		createExpense("1.00", "1999-12-31").andExpect(status().isBadRequest());
		createExpense("1.00", "2000-01-01").andExpect(status().isCreated());
		createExpense("1.00", LocalDate.now().plusYears(2).toString()).andExpect(status().isBadRequest());
		createExpense("1.00", LocalDate.now().plusMonths(6).toString()).andExpect(status().isCreated());
		createExpense("1.00", "03/11/2026").andExpect(status().isBadRequest());
		createExpense("1.00", "2026-02-30").andExpect(status().isBadRequest());
	}

	@Test
	void tipoDiferenteDoDaCategoria_devolve400() throws Exception {
		create(ana, json(anaConta, anaSalario, null, "EXPENSE", "10.00", "2026-11-03", null, null))
				.andExpect(status().isBadRequest());
		create(ana, json(anaConta, anaMercado, null, "INCOME", "10.00", "2026-11-03", null, null))
				.andExpect(status().isBadRequest());
	}

	@Test
	void textosLongosDemais_devolve400() throws Exception {
		create(ana, json(anaConta, anaMercado, null, "EXPENSE", "1.00", "2026-11-03", "m".repeat(201), null))
				.andExpect(status().isBadRequest());
		create(ana, json(anaConta, anaMercado, null, "EXPENSE", "1.00", "2026-11-03", null, "d".repeat(501)))
				.andExpect(status().isBadRequest());
		create(ana, json(anaConta, anaMercado, null, "EXPENSE", "1.00", "2026-11-03", "m".repeat(200),
				"d".repeat(500))).andExpect(status().isCreated());
	}

	// ---------- filtros ----------

	private void seedConjunto() {
		seed(anaMercado, "EXPENSE", "50.00", "2026-11-01", "Supermercado Extra", null);
		seed(anaAlimentacao, "EXPENSE", "20.00", "2026-11-05", "Padaria Quente", null);
		seed(anaSalario, "INCOME", "3000.00", "2026-11-05", "Empresa", "Pagamento do mês");
		seed(anaLazer, "EXPENSE", "80.00", "2026-10-15", "Cinema", "100% diversão");
	}

	@Test
	void listagemOrdenadaPorDataDecrescente_comDesempatePelaCriacao() throws Exception {
		seedConjunto();
		String body = list(ana, "");
		assertThat(JsonPath.<List<String>>read(body, "$.content[*].occurredOn"))
				.containsExactly("2026-11-05", "2026-11-05", "2026-11-01", "2026-10-15");
		assertThat(JsonPath.<List<String>>read(body, "$.content[*].merchant"))
				.containsExactly("Empresa", "Padaria Quente", "Supermercado Extra", "Cinema");
	}

	@Test
	void filtroPorPeriodo_inclusivoNosDoisLados() throws Exception {
		seedConjunto();
		assertThat(total(ana, "?from=2026-11-01&to=2026-11-30")).isEqualTo(3);
		assertThat(total(ana, "?from=2026-11-05&to=2026-11-05")).isEqualTo(2);
		assertThat(total(ana, "?from=2026-11-06")).isZero();
		assertThat(total(ana, "?to=2026-10-31")).isEqualTo(1);
		mvc.perform(get(BASE + "?from=2026-11-30&to=2026-11-01").header("Authorization", ana.bearer()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void filtroPorTipoEPorConta() throws Exception {
		seedConjunto();
		assertThat(total(ana, "?type=EXPENSE")).isEqualTo(3);
		assertThat(total(ana, "?type=INCOME")).isEqualTo(1);
		assertThat(total(ana, "?accountId=" + anaConta)).isEqualTo(4);
		assertThat(total(ana, "?accountId=" + UUID.randomUUID())).isZero();
		mvc.perform(get(BASE + "?type=OUTRO").header("Authorization", ana.bearer()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void filtroPorCategoriaPai_incluiAsSubcategorias() throws Exception {
		seedConjunto();
		assertThat(total(ana, "?categoryId=" + anaAlimentacao)).isEqualTo(2); // Alimentação + Mercado
		assertThat(total(ana, "?categoryId=" + anaMercado)).isEqualTo(1);
		assertThat(total(ana, "?categoryId=" + anaLazer)).isEqualTo(1);
	}

	@Test
	void filtroDeTexto_semDiferenciarCaixa_emEstabelecimentoEDescricao_comCoringasTratadosComoTexto() throws Exception {
		seedConjunto();
		assertThat(total(ana, "?q=PADARIA")).isEqualTo(1);
		assertThat(total(ana, "?q=cinema")).isEqualTo(1);
		assertThat(total(ana, "?q=pagamento")).isEqualTo(1); // casa na descrição
		// "%" e "_" literais (passados como parâmetro, sem passar pelo template de URI do MockMvc)
		assertThat(totalByText("%")).isEqualTo(1); // só a descrição "100% diversão"; sem escape traria os 4
		assertThat(totalByText("_")).isZero(); // sem escape casaria qualquer texto
		assertThat(total(ana, "?q=inexistente")).isZero();
	}

	@Test
	void filtrosCombinados() throws Exception {
		seedConjunto();
		assertThat(total(ana, "?type=EXPENSE&from=2026-11-01&categoryId=" + anaAlimentacao + "&q=pad")).isEqualTo(1);
		assertThat(total(ana, "?type=EXPENSE&from=2026-11-01&to=2026-11-04&accountId=" + anaConta)).isEqualTo(1);
		assertThat(total(ana, "?type=INCOME&categoryId=" + anaLazer)).isZero();
	}

	// ---------- paginação ----------

	@Test
	void paginacao_padrao20_maximo100_totalCorreto_esemRepetirNemPularNaMesmaData() throws Exception {
		for (int i = 0; i < 25; i++) {
			seed(anaMercado, "EXPENSE", "1.00", "2026-11-01", "Item " + i, null); // todas na mesma data
		}
		String p0 = list(ana, "");
		assertThat(JsonPath.<List<?>>read(p0, "$.content")).hasSize(20);
		assertThat(JsonPath.<Integer>read(p0, "$.totalElements")).isEqualTo(25);
		assertThat(JsonPath.<Integer>read(p0, "$.totalPages")).isEqualTo(2);
		assertThat(JsonPath.<Integer>read(p0, "$.size")).isEqualTo(20);
		assertThat(JsonPath.<List<?>>read(list(ana, "?page=1"), "$.content")).hasSize(5);

		assertThat(JsonPath.<Integer>read(list(ana, "?size=500"), "$.size")).isEqualTo(100);

		List<String> ids = new ArrayList<>();
		for (int page = 0; page < 3; page++) {
			ids.addAll(JsonPath.<List<String>>read(list(ana, "?size=10&page=" + page), "$.content[*].id"));
		}
		assertThat(ids).hasSize(25).doesNotHaveDuplicates();

		mvc.perform(get(BASE + "?size=0").header("Authorization", ana.bearer())).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + "?page=-1").header("Authorization", ana.bearer())).andExpect(status().isBadRequest());
		assertThat(JsonPath.<List<?>>read(list(ana, "?page=9"), "$.content")).isEmpty();
	}

	// ---------- isolamento entre usuários ----------

	@Test
	void acessoCruzado_usuarioBNaoLeAlteraNemApagaTransacaoDeA_resposta404() throws Exception {
		String id = idOf(create(ana, json(anaConta, anaMercado, anaPix, "EXPENSE", "99.90", "2026-11-03", "Segredo",
				null)).andExpect(status().isCreated()));
		UUID betoConta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, beto.userId());
		String betoBody = json(betoConta, category(beto, "Mercado"), null, "EXPENSE", "1.00", "2026-11-03", null, null);

		mvc.perform(get(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(put(BASE + "/" + id).header("Authorization", beto.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(betoBody)).andExpect(status().isNotFound());
		mvc.perform(delete(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());

		// A transação de A segue intacta
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.amount").value(99.90)).andExpect(jsonPath("$.merchant").value("Segredo"));
	}

	@Test
	void usuarioBNaoConsegueUsarConta_categoriaOuFormaDePagamentoDeA_resposta404() throws Exception {
		UUID betoConta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, beto.userId());
		UUID betoMercado = category(beto, "Mercado");
		UUID betoPix = jdbc.queryForObject("select id from payment_methods where user_id = ? and name = 'Pix'",
				UUID.class, beto.userId());

		create(beto, json(anaConta, betoMercado, null, "EXPENSE", "1.00", "2026-11-03", null, null))
				.andExpect(status().isNotFound()); // conta de A
		create(beto, json(betoConta, anaMercado, null, "EXPENSE", "1.00", "2026-11-03", null, null))
				.andExpect(status().isNotFound()); // categoria de A
		create(beto, json(betoConta, betoMercado, anaPix, "EXPENSE", "1.00", "2026-11-03", null, null))
				.andExpect(status().isNotFound()); // forma de pagamento de A

		// Também na edição de uma transação do próprio B
		String id = idOf(create(beto, json(betoConta, betoMercado, betoPix, "EXPENSE", "1.00", "2026-11-03", null,
				null)).andExpect(status().isCreated()));
		mvc.perform(put(BASE + "/" + id).header("Authorization", beto.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(json(anaConta, betoMercado, null, "EXPENSE", "1.00", "2026-11-03", null, null)))
				.andExpect(status().isNotFound());

		assertThat(jdbc.queryForObject("select count(*) from transactions where user_id = ?", Integer.class,
				ana.userId())).isZero();
	}

	@Test
	void listagemDeBNuncaTrazDadosDeA_nemFiltrandoPorIdsDeA() throws Exception {
		seedConjunto();

		assertThat(total(beto, "")).isZero();
		assertThat(total(beto, "?categoryId=" + anaMercado)).isZero();
		assertThat(total(beto, "?categoryId=" + anaAlimentacao)).isZero();
		assertThat(total(beto, "?accountId=" + anaConta)).isZero();
		assertThat(total(beto, "?q=Supermercado")).isZero();
		assertThat(total(beto, "?from=2000-01-01&type=EXPENSE")).isZero();
		assertThat(total(ana, "")).isEqualTo(4);
	}

	@Test
	void userIdNoCorpoEhIgnorado_transacaoFicaComODonoDoToken() throws Exception {
		String body = json(anaConta, anaMercado, null, "EXPENSE", "1.00", "2026-11-03", null, null).replaceFirst("\\{",
				"{\"userId\":\"" + beto.userId() + "\",");
		String id = idOf(create(ana, body).andExpect(status().isCreated()));

		assertThat(jdbc.queryForObject("select user_id from transactions where id = ?", UUID.class,
				UUID.fromString(id))).isEqualTo(ana.userId());
	}

	@Test
	void semToken_devolve401() throws Exception {
		mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}
}
