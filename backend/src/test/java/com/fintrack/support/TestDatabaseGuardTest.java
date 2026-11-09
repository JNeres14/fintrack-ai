package com.fintrack.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDatabaseGuardTest {

	@Test
	void aceitaSomenteOBancoDeTeste() {
		assertThatCode(() -> TestDatabaseGuard.assertTestDatabaseName("fintrack_test")).doesNotThrowAnyException();
	}

	@Test
	void recusaOBancoDeDesenvolvimento() {
		assertThatThrownBy(() -> TestDatabaseGuard.assertTestDatabaseName("fintrack"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("fintrack_test");
	}

	@Test
	void recusaQualquerOutroNomeInclusiveVazio() {
		assertThatThrownBy(() -> TestDatabaseGuard.assertTestDatabaseName("postgres"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> TestDatabaseGuard.assertTestDatabaseName(null))
				.isInstanceOf(IllegalStateException.class);
	}
}
