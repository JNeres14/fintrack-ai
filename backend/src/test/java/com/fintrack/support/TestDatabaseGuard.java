package com.fintrack.support;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Trava de segurança: qualquer operação destrutiva nos testes (clean, truncate) só roda se o banco
 * conectado for exatamente o de teste. Evita apagar o banco de desenvolvimento por engano.
 */
public final class TestDatabaseGuard {

	public static final String TEST_DATABASE = "fintrack_test";

	private TestDatabaseGuard() {
	}

	public static void assertTestDatabase(DataSource dataSource) {
		String current = new JdbcTemplate(dataSource).queryForObject("select current_database()", String.class);
		assertTestDatabaseName(current);
	}

	static void assertTestDatabaseName(String databaseName) {
		if (!TEST_DATABASE.equals(databaseName)) {
			throw new IllegalStateException("Operação destrutiva recusada: os testes só podem rodar no banco '"
					+ TEST_DATABASE + "', mas a conexão aponta para '" + databaseName + "'. "
					+ "Confira DB_TEST_URL no .env.");
		}
	}
}
