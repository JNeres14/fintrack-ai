package com.fintrack.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Confere, direto por SQL, que as migrações rodaram do zero e que as regras do schema valem. */
@SpringBootTest
@Transactional
class DatabaseSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Flyway flyway;

	@Autowired
	private DataSource dataSource;

	private UUID userA;
	private UUID userB;
	private UUID accountA;
	private UUID categoryA;

	@BeforeEach
	void dadosBase() {
		userA = insertUser("a@teste.com");
		userB = insertUser("b@teste.com");
		accountA = insertAccount(userA, "Carteira");
		categoryA = insertCategory(userA, "Alimentação", null);
	}

	@Test
	void conectaNoBancoDeTesteEMigracaoV1Aplicada() {
		TestDatabaseGuard.assertTestDatabase(dataSource);
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
		List<String> tables = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public'", String.class);
		assertThat(tables).contains("users", "accounts", "categories", "payment_methods", "transactions",
				"budgets", "refresh_tokens", "invoice_payments", "flyway_schema_history");
	}

	@Test
	void valorMonetarioUsaNumeric14e2() {
		String type = jdbc.queryForObject("""
				select numeric_precision || ',' || numeric_scale from information_schema.columns
				where table_name = 'transactions' and column_name = 'amount'""", String.class);
		assertThat(type).isEqualTo("14,2");
	}

	@Test
	void recusaValorZero() {
		assertThatThrownBy(() -> insertTransaction(userA, accountA, categoryA, "EXPENSE", "0.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaValorNegativo() {
		assertThatThrownBy(() -> insertTransaction(userA, accountA, categoryA, "EXPENSE", "-5.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaTipoInvalido() {
		assertThatThrownBy(() -> insertTransaction(userA, accountA, categoryA, "TRANSFER", "10.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void arredondaParaDuasCasasNoBanco() {
		insertTransaction(userA, accountA, categoryA, "EXPENSE", "10.456");
		String amount = jdbc.queryForObject("select amount::text from transactions", String.class);
		assertThat(amount).isEqualTo("10.46");
	}

	@Test
	void recusaTransacaoComContaDeOutroUsuario() {
		UUID accountB = insertAccount(userB, "Conta do B");
		assertThatThrownBy(() -> insertTransaction(userA, accountB, categoryA, "EXPENSE", "10.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaTransacaoComCategoriaDeOutroUsuario() {
		UUID categoryB = insertCategory(userB, "Categoria do B", null);
		assertThatThrownBy(() -> insertTransaction(userA, accountA, categoryB, "EXPENSE", "10.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaSubcategoriaComPaiDeOutroUsuario() {
		UUID categoryB = insertCategory(userB, "Categoria do B", null);
		assertThatThrownBy(() -> insertCategory(userA, "Filha", categoryB))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aceitaSubcategoriaDoMesmoUsuario() {
		UUID child = insertCategory(userA, "Restaurantes", categoryA);
		assertThat(child).isNotNull();
	}

	@Test
	void recusaEmailDuplicadoEEmailComMaiusculas() {
		assertThatThrownBy(() -> insertUser("a@teste.com")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaEmailComMaiusculas() {
		assertThatThrownBy(() -> insertUser("Maiuscula@Teste.com")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void nomeDeCategoriaUnicoPorUsuarioNoMesmoNivel() {
		assertThatThrownBy(() -> insertCategory(userA, "alimentação", null))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void mesmoNomeEmUsuariosDiferentesEPermitido() {
		assertThat(insertCategory(userB, "Alimentação", null)).isNotNull();
	}

	@Test
	void orcamentoExigeDiaUmDoMes() {
		assertThatThrownBy(() -> insertBudget(userA, null, "500.00", "2026-11-15"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void orcamentoGeralUnicoPorMes() {
		insertBudget(userA, null, "500.00", "2026-11-01");
		assertThatThrownBy(() -> insertBudget(userA, null, "700.00", "2026-11-01"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void orcamentoPorCategoriaUnicoPorMesMasConvivecomOGeral() {
		insertBudget(userA, null, "500.00", "2026-11-01");
		insertBudget(userA, categoryA, "200.00", "2026-11-01");
		assertThatThrownBy(() -> insertBudget(userA, categoryA, "300.00", "2026-11-01"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void orcamentoExigeLimitePositivo() {
		assertThatThrownBy(() -> insertBudget(userA, null, "0.00", "2026-11-01"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void recusaExcluirContaComTransacoes() {
		insertTransaction(userA, accountA, categoryA, "EXPENSE", "10.00");
		assertThatThrownBy(() -> jdbc.update("delete from accounts where id = ?", accountA))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	// ---- auxiliares ----

	private UUID insertUser(String email) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into users (id, email, password_hash) values (?, ?, 'hash-de-teste')", id, email);
		return id;
	}

	private UUID insertAccount(UUID userId, String name) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into accounts (id, user_id, name, type) values (?, ?, ?, 'WALLET')", id, userId, name);
		return id;
	}

	private UUID insertCategory(UUID userId, String name, UUID parentId) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into categories (id, user_id, name, kind, parent_id) values (?, ?, ?, 'EXPENSE', ?)", id,
				userId, name, parentId);
		return id;
	}

	private void insertTransaction(UUID userId, UUID accountId, UUID categoryId, String type, String amount) {
		jdbc.update("""
				insert into transactions (user_id, account_id, category_id, type, amount, occurred_on)
				values (?, ?, ?, ?, ?::numeric, current_date)""", userId, accountId, categoryId, type, amount);
	}

	private void insertBudget(UUID userId, UUID categoryId, String limit, String month) {
		jdbc.update("""
				insert into budgets (user_id, category_id, limit_amount, month)
				values (?, ?, ?::numeric, ?::date)""", userId, categoryId, limit, month);
	}
}
