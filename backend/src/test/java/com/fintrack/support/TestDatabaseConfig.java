package com.fintrack.support;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ao subir o contexto de teste: confere o banco, apaga o schema e roda as migrações do zero.
 * Só existe no perfil test e só em src/test.
 */
@Configuration
@Profile("test")
public class TestDatabaseConfig {

	@Bean
	FlywayMigrationStrategy cleanThenMigrate(DataSource dataSource) {
		return (Flyway flyway) -> {
			TestDatabaseGuard.assertTestDatabase(dataSource);
			flyway.clean();
			flyway.migrate();
		};
	}

	/** Limpa os dados entre testes de fluxo completo (os de repository usam rollback). Também protegido. */
	@Bean
	TestDatabaseCleaner testDatabaseCleaner(DataSource dataSource) {
		return new TestDatabaseCleaner(dataSource);
	}

	public static class TestDatabaseCleaner {

		private final DataSource dataSource;

		TestDatabaseCleaner(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		public void truncateAll() {
			TestDatabaseGuard.assertTestDatabase(dataSource);
			new JdbcTemplate(dataSource).execute(
					"TRUNCATE transactions, budgets, categories, payment_methods, accounts, users CASCADE");
		}
	}
}
