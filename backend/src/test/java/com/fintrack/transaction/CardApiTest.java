package com.fintrack.transaction;

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

/**
 * Cartão, fatura e pagamento (ADR-016). A data "de hoje" é controlada pelo teste (AppToday simulado), para
 * testar o fechamento no dia exato e no dia seguinte sem depender do relógio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CardApiTest {

	private static final LocalDate HOJE = LocalDate.of(2026, 12, 26); // um dia depois do fechamento (dia 25)

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
	private UUID cartao; // do Ana: fecha dia 25, vence dia 5, limite 5000.00
	private UUID carteira; // conta padrão do Ana (origem dos pagamentos)

	@BeforeEach
	void prepara() throws Exception {
		cleaner.truncateAll();
		when(today.today()).thenReturn(HOJE);
		ana = TestUsers.create(mvc, "ana@teste.com");
		beto = TestUsers.create(mvc, "beto@teste.com");
		carteira = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, ana.userId());
		cartao = idOf(createCard(ana, "Nubank", "5000.00", 25, 5).andExpect(status().isCreated()));
	}

	// ---------- utilitários ----------

	private ResultActions createCard(Session s, String name, String limit, Integer closing, Integer due)
			throws Exception {
		String body = "{\"name\":\"" + name + "\",\"type\":\"CREDIT_CARD\",\"creditLimit\":" + limit
				+ ",\"closingDay\":" + closing + ",\"dueDay\":" + due + "}";
		return mvc.perform(post("/api/accounts").header("Authorization", s.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private UUID idOf(ResultActions r) throws Exception {
		return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
	}

	private void seed(Session s, UUID account, String type, String amount, String date) {
		UUID category = jdbc.queryForObject("select id from categories where user_id = ? and name = 'Mercado'",
				UUID.class, s.userId());
		jdbc.update("insert into transactions (user_id, account_id, category_id, type, amount, occurred_on) "
				+ "values (?, ?, ?, ?, ?, ?)", s.userId(), account, category, type, new BigDecimal(amount),
				LocalDate.parse(date));
	}

	/** Conjunto do Ana no cartão (fecha 25, vence 5). Fatura 2026-12 = 40.10 + 59.90 - 10.00 + 0.01 = 90.01. */
	private void seedAna() {
		seed(ana, cartao, "EXPENSE", "100.00", "2026-11-25"); // fatura 2026-11 (último dia dela)
		seed(ana, cartao, "EXPENSE", "40.10", "2026-11-26"); // fatura 2026-12 (primeiro dia dela)
		seed(ana, cartao, "EXPENSE", "59.90", "2026-12-05");
		seed(ana, cartao, "INCOME", "10.00", "2026-12-10"); // estorno
		seed(ana, cartao, "EXPENSE", "0.01", "2026-12-25"); // fatura 2026-12 (dia do fechamento, inclusive)
		seed(ana, cartao, "EXPENSE", "70.00", "2026-12-26"); // fatura 2027-01
	}

	private ResultActions invoice(Session s, UUID card, String month) throws Exception {
		return mvc.perform(get("/api/accounts/" + card + "/invoices/" + month).header("Authorization", s.bearer()));
	}

	private ResultActions pay(Session s, UUID card, String month, UUID source, String amount, String paidOn)
			throws Exception {
		String body = "{\"sourceAccountId\":\"" + source + "\",\"amount\":" + amount
				+ (paidOn == null ? "" : ",\"paidOn\":\"" + paidOn + "\"") + "}";
		return mvc.perform(post("/api/accounts/" + card + "/invoices/" + month + "/payments")
				.header("Authorization", s.bearer()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String txJson(UUID account, String type, String amount, String date) {
		UUID category = jdbc.queryForObject("select id from categories where user_id = ? and name = ?", UUID.class,
				ana.userId(), "INCOME".equals(type) ? "Salário" : "Mercado");
		return "{\"accountId\":\"" + account + "\",\"categoryId\":\"" + category + "\",\"type\":\"" + type
				+ "\",\"amount\":" + amount + ",\"occurredOn\":\"" + date + "\"}";
	}

	private ResultActions createTx(String json) throws Exception {
		return mvc.perform(post("/api/transactions").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private UUID txId(String date) {
		return jdbc.queryForObject("select id from transactions where account_id = ? and occurred_on = ? limit 1",
				UUID.class, cartao, LocalDate.parse(date));
	}

	// ---------- cadastro do cartão ----------

	@Test
	void cartaoGuardaLimiteEDias_eContaComumNaoPodeTerCamposDeCartao() throws Exception {
		mvc.perform(get("/api/accounts/" + cartao).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.type").value("CREDIT_CARD"))
				.andExpect(jsonPath("$.creditLimit").value(5000.00))
				.andExpect(jsonPath("$.closingDay").value(25))
				.andExpect(jsonPath("$.dueDay").value(5));

		String semCampos = "{\"name\":\"X\",\"type\":\"CREDIT_CARD\"}";
		mvc.perform(post("/api/accounts").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(semCampos)).andExpect(status().isBadRequest());
		String comumComCampos = "{\"name\":\"Y\",\"type\":\"CHECKING\",\"creditLimit\":100.00,\"closingDay\":10,\"dueDay\":20}";
		mvc.perform(post("/api/accounts").header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(comumComCampos)).andExpect(status().isBadRequest());
	}

	@Test
	void diasForaDeUmAVinteEOito_limiteNegativoOuComTresCasas_devolve400() throws Exception {
		createCard(ana, "A", "100.00", 0, 10).andExpect(status().isBadRequest());
		createCard(ana, "B", "100.00", 10, 29).andExpect(status().isBadRequest());
		createCard(ana, "C", "-1.00", 10, 10).andExpect(status().isBadRequest());
		createCard(ana, "D", "10.123", 10, 10).andExpect(status().isBadRequest());
		createCard(ana, "E", "0.00", 1, 28).andExpect(status().isCreated()); // limite zero e dias extremos: ok
	}

	// ---------- fatura: período, total e datas ----------

	@Test
	void compraNoDiaDoFechamentoEntraNaFaturaDoMes_eNoDiaSeguinteNaProxima() throws Exception {
		seedAna();

		invoice(ana, cartao, "2026-12").andExpect(status().isOk())
				.andExpect(jsonPath("$.invoice.month").value("2026-12"))
				.andExpect(jsonPath("$.invoice.periodStart").value("2026-11-26"))
				.andExpect(jsonPath("$.invoice.closingDate").value("2026-12-25"))
				.andExpect(jsonPath("$.invoice.dueDate").value("2027-01-05")) // vence no mês seguinte (5 <= 25)
				.andExpect(jsonPath("$.invoice.total").value(90.01)) // 40.10 + 59.90 - 10.00 + 0.01
				.andExpect(jsonPath("$.transactions.length()").value(4));

		invoice(ana, cartao, "2026-11")
				.andExpect(jsonPath("$.invoice.total").value(100.00))
				.andExpect(jsonPath("$.invoice.periodStart").value("2026-10-26"))
				.andExpect(jsonPath("$.invoice.dueDate").value("2026-12-05"))
				.andExpect(jsonPath("$.transactions.length()").value(1));

		invoice(ana, cartao, "2027-01") // virada de ano: a compra de 26/12 caiu aqui
				.andExpect(jsonPath("$.invoice.total").value(70.00))
				.andExpect(jsonPath("$.invoice.periodStart").value("2026-12-26"))
				.andExpect(jsonPath("$.invoice.closingDate").value("2027-01-25"))
				.andExpect(jsonPath("$.invoice.dueDate").value("2027-02-05"));
	}

	@Test
	void vencimentoNoMesmoMes_quandoDiaDeVencimentoEhMaiorQueODeFechamento() throws Exception {
		UUID outro = idOf(createCard(ana, "Inter", "1000.00", 5, 10).andExpect(status().isCreated()));

		invoice(ana, outro, "2026-12")
				.andExpect(jsonPath("$.invoice.periodStart").value("2026-11-06"))
				.andExpect(jsonPath("$.invoice.closingDate").value("2026-12-05"))
				.andExpect(jsonPath("$.invoice.dueDate").value("2026-12-10"));

		UUID igual = idOf(createCard(ana, "Itaú", "1000.00", 10, 10).andExpect(status().isCreated()));
		invoice(ana, igual, "2026-12").andExpect(jsonPath("$.invoice.dueDate").value("2027-01-10"));
	}

	// ---------- status ----------

	@Test
	void statusAberta_noDiaDoFechamento_fechada_noDiaSeguinte_ePaga() throws Exception {
		seedAna();

		when(today.today()).thenReturn(LocalDate.of(2026, 12, 25)); // dia do fechamento: ainda aberta
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("OPEN"));

		when(today.today()).thenReturn(LocalDate.of(2026, 12, 26)); // dia seguinte: fechada
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("CLOSED"))
				.andExpect(jsonPath("$.invoice.remaining").value(90.01));

		pay(ana, cartao, "2026-12", carteira, "90.01", null).andExpect(status().isCreated());
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("PAID"))
				.andExpect(jsonPath("$.invoice.remaining").value(0.0));
	}

	// ---------- pagamento ----------

	@Test
	void pagamentoParcialEDepoisTotal_somaOsPagamentos() throws Exception {
		seedAna();

		pay(ana, cartao, "2026-12", carteira, "30.00", "2026-12-26").andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceMonth").value("2026-12"))
				.andExpect(jsonPath("$.amount").value(30.00));
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("CLOSED"))
				.andExpect(jsonPath("$.invoice.paid").value(30.00))
				.andExpect(jsonPath("$.invoice.remaining").value(60.01))
				.andExpect(jsonPath("$.payments.length()").value(1));

		pay(ana, cartao, "2026-12", carteira, "60.01", null).andExpect(status().isCreated());
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("PAID"))
				.andExpect(jsonPath("$.payments.length()").value(2));
		pay(ana, cartao, "2026-12", carteira, "0.01", null).andExpect(status().isBadRequest()); // nada mais a pagar
	}

	@Test
	void pagamentoRecusado_faturaAberta_valorMaiorQueOSaldo_faturaZerada_origemInvalida_dataInvalida() throws Exception {
		seedAna();

		pay(ana, cartao, "2027-01", carteira, "10.00", null).andExpect(status().isBadRequest()); // fatura aberta
		pay(ana, cartao, "2026-12", carteira, "90.02", null).andExpect(status().isBadRequest()); // maior que o saldo
		pay(ana, cartao, "2026-09", carteira, "1.00", null).andExpect(status().isBadRequest()); // fatura sem valor
		pay(ana, cartao, "2026-12", cartao, "10.00", null).andExpect(status().isBadRequest()); // origem é o próprio cartão
		UUID outroCartao = idOf(createCard(ana, "Inter", "1000.00", 5, 10).andExpect(status().isCreated()));
		pay(ana, cartao, "2026-12", outroCartao, "10.00", null).andExpect(status().isBadRequest()); // origem é cartão
		pay(ana, cartao, "2026-12", carteira, "10.00", "2026-12-24").andExpect(status().isBadRequest()); // antes do fechamento
		pay(ana, cartao, "2026-12", carteira, "10.00", "2026-12-27").andExpect(status().isBadRequest()); // no futuro
		pay(ana, cartao, "2026-12", carteira, "0", null).andExpect(status().isBadRequest());
		pay(ana, cartao, "2026-12", carteira, "10.005", null).andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("select count(*) from invoice_payments", Integer.class)).isZero();

		pay(ana, cartao, "2026-12", carteira, "10.00", "2026-12-25").andExpect(status().isCreated()); // no fechamento: ok
	}

	@Test
	void saldos_pagamentoReduzOrigemEDividaDoCartao_eNaoViraGastoNemReceita() throws Exception {
		seedAna();
		pay(ana, cartao, "2026-12", carteira, "90.01", null).andExpect(status().isCreated());

		// cartão: -260.01 (10.00 de estorno - 270.01 de compras) + 90.01 pagos = -170.00
		mvc.perform(get("/api/accounts/" + cartao).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.balance").value(-170.00));
		mvc.perform(get("/api/accounts/" + carteira).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.balance").value(-90.01));
		String lista = mvc.perform(get("/api/accounts").header("Authorization", ana.bearer()))
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<Double>>read(lista, "$[?(@.name=='Carteira')].balance")).containsExactly(-90.01);

		// o pagamento não aparece como transação em lugar nenhum
		mvc.perform(get("/api/transactions?accountId=" + carteira).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/transactions?type=INCOME").header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.totalElements").value(1)); // só o estorno
		assertThat(jdbc.queryForObject("select count(*) from transactions", Integer.class)).isEqualTo(6);
	}

	@Test
	void excluirPagamento_devolveOSaldo_eLiberaAFatura() throws Exception {
		seedAna();
		UUID pagamento = UUID.fromString(JsonPath.read(pay(ana, cartao, "2026-12", carteira, "90.01", null)
				.andReturn().getResponse().getContentAsString(), "$.id"));

		mvc.perform(delete("/api/accounts/" + cartao + "/invoices/2026-12/payments/" + pagamento)
				.header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		mvc.perform(get("/api/accounts/" + carteira).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.balance").value(0.0));
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.status").value("CLOSED"));
		mvc.perform(delete("/api/accounts/" + cartao + "/invoices/2026-12/payments/" + pagamento)
				.header("Authorization", ana.bearer())).andExpect(status().isNotFound());
	}

	// ---------- compra em fatura com pagamento ----------

	@Test
	void compraEmFaturaComPagamento_naoPodeSerCriadaEditadaNemExcluida_mesmoComPagamentoParcial() throws Exception {
		seedAna();
		pay(ana, cartao, "2026-12", carteira, "30.00", null).andExpect(status().isCreated()); // parcial

		createTx(txJson(cartao, "EXPENSE", "5.00", "2026-12-20")).andExpect(status().isConflict()); // criar
		UUID dentro = txId("2026-12-05");
		mvc.perform(put("/api/transactions/" + dentro).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(txJson(cartao, "EXPENSE", "1.00", "2026-12-05")))
				.andExpect(status().isConflict()); // editar
		mvc.perform(delete("/api/transactions/" + dentro).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict()); // excluir

		// mover uma compra de fatura sem pagamento (2027-01) para dentro da paga: bloqueado
		UUID deJaneiro = txId("2026-12-26");
		mvc.perform(put("/api/transactions/" + deJaneiro).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(txJson(cartao, "EXPENSE", "70.00", "2026-12-20")))
				.andExpect(status().isConflict());
		// e tirar da paga para outra: bloqueado também (a de origem tem pagamento)
		mvc.perform(put("/api/transactions/" + dentro).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(txJson(cartao, "EXPENSE", "59.90", "2026-12-28")))
				.andExpect(status().isConflict());

		// nada mudou
		assertThat(jdbc.queryForObject("select count(*) from transactions where account_id = ?", Integer.class, cartao))
				.isEqualTo(6);
		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.total").value(90.01));
	}

	@Test
	void faturaSemPagamento_podeSerCorrigida_mesmoFechada_eOutrasFaturasSeguemLivres() throws Exception {
		seedAna();
		pay(ana, cartao, "2026-12", carteira, "30.00", null).andExpect(status().isCreated());

		createTx(txJson(cartao, "EXPENSE", "5.00", "2026-12-27")).andExpect(status().isCreated()); // fatura 2027-01
		UUID novembro = txId("2026-11-25"); // fatura 2026-11: fechada, sem pagamento
		mvc.perform(put("/api/transactions/" + novembro).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(txJson(cartao, "EXPENSE", "101.00", "2026-11-25")))
				.andExpect(status().isOk());
		invoice(ana, cartao, "2026-11").andExpect(jsonPath("$.invoice.total").value(101.00));

		// apagado o pagamento, a fatura de dezembro volta a aceitar alteração
		UUID pagamento = jdbc.queryForObject("select id from invoice_payments", UUID.class);
		mvc.perform(delete("/api/accounts/" + cartao + "/invoices/2026-12/payments/" + pagamento)
				.header("Authorization", ana.bearer())).andExpect(status().isNoContent());
		createTx(txJson(cartao, "EXPENSE", "5.00", "2026-12-20")).andExpect(status().isCreated());
	}

	@Test
	void contaComum_naoSofreOBloqueio() throws Exception {
		seedAna();
		pay(ana, cartao, "2026-12", carteira, "90.01", null).andExpect(status().isCreated());

		createTx(txJson(carteira, "EXPENSE", "5.00", "2026-12-20")).andExpect(status().isCreated());
	}

	// ---------- regras da conta do cartão ----------

	@Test
	void comPagamento_naoMudaODiaDeFechamento_masMudaVencimentoELimite_eNaoTrocaTipoNemExclui() throws Exception {
		seedAna();
		pay(ana, cartao, "2026-12", carteira, "30.00", null).andExpect(status().isCreated());
		String base = "{\"name\":\"Nubank\",\"type\":\"CREDIT_CARD\",\"creditLimit\":%s,\"closingDay\":%d,\"dueDay\":%d}";

		mvc.perform(put("/api/accounts/" + cartao).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(base.formatted("5000.00", 20, 5)))
				.andExpect(status().isConflict()); // fechamento 25 -> 20
		mvc.perform(put("/api/accounts/" + cartao).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(base.formatted("8000.00", 25, 12)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.creditLimit").value(8000.00))
				.andExpect(jsonPath("$.dueDay").value(12));

		mvc.perform(put("/api/accounts/" + cartao).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nubank\",\"type\":\"CHECKING\"}"))
				.andExpect(status().isConflict()); // deixar de ser cartão com movimentação
		mvc.perform(delete("/api/accounts/" + cartao).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict());
		mvc.perform(delete("/api/accounts/" + carteira).header("Authorization", ana.bearer()))
				.andExpect(status().isConflict()); // origem de pagamento
	}

	@Test
	void semPagamento_mudarODiaDeFechamentoReclassificaAsCompras() throws Exception {
		seedAna();
		String corpo = "{\"name\":\"Nubank\",\"type\":\"CREDIT_CARD\",\"creditLimit\":5000.00,\"closingDay\":28,\"dueDay\":5}";
		mvc.perform(put("/api/accounts/" + cartao).header("Authorization", ana.bearer())
				.contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());

		invoice(ana, cartao, "2026-12").andExpect(jsonPath("$.invoice.periodStart").value("2026-11-29"))
				.andExpect(jsonPath("$.invoice.closingDate").value("2026-12-28"))
				.andExpect(jsonPath("$.invoice.total").value(119.91)); // 59.90 - 10.00 + 0.01 + 70.00 (a de 26/11 saiu)
	}

	// ---------- lista de cartões e limite ----------

	@Test
	void listaDeCartoes_limiteUsadoIncluiTudoQueNaoFoiPago_eComprasFuturas() throws Exception {
		seedAna();
		seed(ana, cartao, "EXPENSE", "30.00", "2027-03-10"); // compra com data futura também consome limite
		pay(ana, cartao, "2026-12", carteira, "90.01", null).andExpect(status().isCreated());

		// dívida: 270.01 + 30.00 - 10.00 (estorno) - 90.01 (pago) = 200.00
		mvc.perform(get("/api/cards").header("Authorization", ana.bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Nubank"))
				.andExpect(jsonPath("$[0].creditLimit").value(5000.00))
				.andExpect(jsonPath("$[0].usedLimit").value(200.00))
				.andExpect(jsonPath("$[0].availableLimit").value(4800.00))
				.andExpect(jsonPath("$[0].balance").value(-200.00))
				.andExpect(jsonPath("$[0].currentInvoice.month").value("2027-01")) // hoje 26/12: já é a de janeiro
				.andExpect(jsonPath("$[0].currentInvoice.status").value("OPEN"))
				.andExpect(jsonPath("$[0].currentInvoice.total").value(70.00))
				.andExpect(jsonPath("$[0].previousInvoice.month").value("2026-12"))
				.andExpect(jsonPath("$[0].previousInvoice.status").value("PAID"));
	}

	@Test
	void limiteDisponivelPodeFicarNegativo_eCartaoSemDivida_temUsadoZero() throws Exception {
		UUID pequeno = idOf(createCard(ana, "Pequeno", "50.00", 10, 20).andExpect(status().isCreated()));
		mvc.perform(get("/api/cards").header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$[?(@.name=='Pequeno')].usedLimit").value(0.0))
				.andExpect(jsonPath("$[?(@.name=='Pequeno')].availableLimit").value(50.0));

		seed(ana, pequeno, "EXPENSE", "80.00", "2026-12-01");
		mvc.perform(get("/api/cards").header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$[?(@.name=='Pequeno')].usedLimit").value(80.0))
				.andExpect(jsonPath("$[?(@.name=='Pequeno')].availableLimit").value(-30.0));
	}

	@Test
	void faturaInvalida_mesMalFormadoOuForaDoIntervalo_ouContaQueNaoEhCartao() throws Exception {
		invoice(ana, cartao, "2026-13").andExpect(status().isBadRequest());
		invoice(ana, cartao, "dezembro").andExpect(status().isBadRequest());
		invoice(ana, cartao, "1999-12").andExpect(status().isBadRequest());
		invoice(ana, cartao, "2099-01").andExpect(status().isBadRequest());
		invoice(ana, carteira, "2026-12").andExpect(status().isNotFound()); // conta comum não tem fatura
		invoice(ana, UUID.randomUUID(), "2026-12").andExpect(status().isNotFound());
	}

	// ---------- isolamento entre usuários ----------

	@Test
	void acessoCruzado_usuarioBNaoVeNemPagaNemExcluiPagamentoDoCartaoDeA_resposta404() throws Exception {
		seedAna();
		UUID pagamentoDoAna = UUID.fromString(JsonPath.read(pay(ana, cartao, "2026-12", carteira, "30.00", null)
				.andReturn().getResponse().getContentAsString(), "$.id"));
		UUID betoConta = jdbc.queryForObject("select id from accounts where user_id = ?", UUID.class, beto.userId());

		invoice(beto, cartao, "2026-12").andExpect(status().isNotFound());
		pay(beto, cartao, "2026-12", betoConta, "10.00", null).andExpect(status().isNotFound());
		mvc.perform(delete("/api/accounts/" + cartao + "/invoices/2026-12/payments/" + pagamentoDoAna)
				.header("Authorization", beto.bearer())).andExpect(status().isNotFound());
		mvc.perform(get("/api/cards").header("Authorization", beto.bearer()))
				.andExpect(jsonPath("$.length()").value(0));
		assertThat(jdbc.queryForObject("select count(*) from invoice_payments", Integer.class)).isEqualTo(1);
	}

	@Test
	void usuarioBNaoUsaContaDeAComoOrigem_nemCartaoDeAComoSeu() throws Exception {
		UUID cartaoDoBeto = idOf(createCard(beto, "Cartão B", "1000.00", 25, 5).andExpect(status().isCreated()));
		seed(beto, cartaoDoBeto, "EXPENSE", "50.00", "2026-12-01");

		pay(beto, cartaoDoBeto, "2026-12", carteira, "10.00", null).andExpect(status().isNotFound()); // origem do Ana
		UUID contaDoBeto = jdbc.queryForObject("select id from accounts where user_id = ? and name = 'Carteira'",
				UUID.class, beto.userId());
		pay(beto, cartaoDoBeto, "2026-12", contaDoBeto, "10.00", null).andExpect(status().isCreated()); // controle

		// Ana não consegue mexer nas compras do cartão do Beto
		createTx(txJson(cartaoDoBeto, "EXPENSE", "1.00", "2026-12-01")).andExpect(status().isNotFound());
	}

	@Test
	void usuarioBNaoEnxergaNemMudaCampoDeCartaoDeA() throws Exception {
		mvc.perform(put("/api/accounts/" + cartao).header("Authorization", beto.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Invadido\",\"type\":\"CREDIT_CARD\",\"creditLimit\":1.00,\"closingDay\":1,\"dueDay\":2}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/accounts/" + cartao).header("Authorization", ana.bearer()))
				.andExpect(jsonPath("$.creditLimit").value(5000.00));
	}

	@Test
	void semToken_devolve401() throws Exception {
		mvc.perform(get("/api/cards")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/accounts/" + cartao + "/invoices/2026-12")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/accounts/" + cartao + "/invoices/2026-12/payments")
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/accounts/" + cartao + "/invoices/2026-12/payments/" + UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}
