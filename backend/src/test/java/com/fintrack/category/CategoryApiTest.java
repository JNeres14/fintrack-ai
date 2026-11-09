package com.fintrack.category;

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
class CategoryApiTest {

	private static final String BASE = "/api/categories";

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

	private ResultActions create(Session s, String name, String kind, UUID parentId) throws Exception {
		String body = "{\"name\":\"" + name + "\",\"kind\":\"" + kind + "\""
				+ (parentId == null ? "" : ",\"parentId\":\"" + parentId + "\"") + "}";
		return mvc.perform(post(BASE).header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private UUID idOf(ResultActions r) throws Exception {
		return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private UUID idByName(Session s, String name) {
		return jdbc.queryForObject("select id from categories where user_id = ? and name = ? limit 1", UUID.class,
				s.userId(), name);
	}

	@Test
	void novoUsuarioRecebeAsCategoriasPadrao_comSubcategorias() throws Exception {
		String list = mvc.perform(get(BASE).header("Authorization", ana.bearer()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<List<String>>read(list, "$[*].name")).hasSize(18)
				.contains("Alimentação", "Mercado", "Restaurantes", "Salário", "Rendimentos");
		assertThat(JsonPath.<List<String>>read(list, "$[?(@.kind=='INCOME')].name")).hasSize(3);
		// "Mercado" é filha de "Alimentação"
		String parent = JsonPath.<List<String>>read(list, "$[?(@.name=='Mercado')].parentId").get(0);
		assertThat(parent).isEqualTo(idByName(ana, "Alimentação").toString());
		// cada usuário tem o seu conjunto, sem misturar
		assertThat(jdbc.queryForObject("select count(*) from categories where user_id = ?", Integer.class,
				beto.userId())).isEqualTo(18);
	}

	@Test
	void crudCompleto_eSubcategoria() throws Exception {
		UUID pai = idOf(create(ana, "Pets", "EXPENSE", null).andExpect(status().isCreated()));
		UUID filha = idOf(create(ana, "Ração", "EXPENSE", pai).andExpect(status().isCreated())
				.andExpect(jsonPath("$.parentId").value(pai.toString())));

		mvc.perform(get(BASE + "/" + filha).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.name").value("Ração"));
		mvc.perform(put(BASE + "/" + filha).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ração e petiscos\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ração e petiscos"))
				.andExpect(jsonPath("$.kind").value("EXPENSE")).andExpect(jsonPath("$.parentId").value(pai.toString()));
		mvc.perform(delete(BASE + "/" + filha).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(delete(BASE + "/" + pai).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
	}

	@Test
	void regrasDeSubcategoria() throws Exception {
		UUID alimentacao = idByName(ana, "Alimentação");
		UUID mercado = idByName(ana, "Mercado");

		create(ana, "Feira", "EXPENSE", mercado).andExpect(status().isBadRequest()); // sub de sub
		create(ana, "Bônus", "INCOME", alimentacao).andExpect(status().isBadRequest()); // tipo diferente do pai
		create(ana, "X", "EXPENSE", UUID.randomUUID()).andExpect(status().isNotFound()); // pai inexistente
		create(ana, "Mercado", "EXPENSE", alimentacao).andExpect(status().isConflict()); // repetida no mesmo pai
	}

	@Test
	void nomeRepetido_soNoMesmoNivelETipo() throws Exception {
		create(ana, "alimentação", "EXPENSE", null).andExpect(status().isConflict()); // sem diferenciar caixa
		create(ana, "Salário", "EXPENSE", null).andExpect(status().isCreated()); // outro tipo: permitido
		create(ana, "Mercado", "EXPENSE", null).andExpect(status().isCreated()); // raiz x filha: permitido
		create(ana, " ", "EXPENSE", null).andExpect(status().isBadRequest());
		create(ana, "Y", "NAO_EXISTE", null).andExpect(status().isBadRequest());
	}

	@Test
	void excluirComSubcategorias_comTransacoes_ouComOrcamento_devolve409() throws Exception {
		mvc.perform(delete(BASE + "/" + idByName(ana, "Alimentação")).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict()); // tem subcategorias

		UUID saude = idByName(ana, "Saúde");
		UUID conta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on) "
				+ "values (?, ?, ?, 'EXPENSE', ?, ?)", ana.userId(), conta, saude, new BigDecimal("12.34"),
				LocalDate.of(2026, 11, 2));
		mvc.perform(delete(BASE + "/" + saude).header("Authorization", ana.bearer())).andExpect(status().isConflict());
		jdbc.update("delete from transactions");

		UUID lazer = idByName(ana, "Lazer");
		jdbc.update("insert into budgets (user_id, category_id, limit_amount, month) values (?, ?, ?, ?)",
				ana.userId(), lazer, new BigDecimal("200.00"), LocalDate.of(2026, 11, 1));
		mvc.perform(delete(BASE + "/" + lazer).header("Authorization", ana.bearer())).andExpect(status().isConflict());
		jdbc.update("delete from budgets");

		mvc.perform(delete(BASE + "/" + saude).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(delete(BASE + "/" + lazer).header("Authorization", ana.bearer())).andExpect(status().isNoContent());
	}

	@Test
	void acessoCruzado_usuarioBNaoLeAlteraNemApagaCategoriaDeA_resposta404() throws Exception {
		UUID id = idOf(create(ana, "Reservada", "EXPENSE", null).andExpect(status().isCreated()));

		mvc.perform(get(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(put(BASE + "/" + id).header("Authorization", beto.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invadida\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(delete(BASE + "/" + id).header("Authorization", beto.bearer())).andExpect(status().isNotFound());

		String lista = mvc.perform(get(BASE).header("Authorization", beto.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(lista).doesNotContain(id.toString()).doesNotContain("Reservada");
		mvc.perform(get(BASE + "/" + id).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.name").value("Reservada"));
	}

	@Test
	void naoDaParaCriarSubcategoriaSobreCategoriaDeOutroUsuario() throws Exception {
		UUID doBeto = idByName(beto, "Lazer");
		create(ana, "Cinema", "EXPENSE", doBeto).andExpect(status().isNotFound());
		assertThat(jdbc.queryForObject("select count(*) from categories where name = 'Cinema'", Integer.class))
				.isZero();
	}
}
