package com.fintrack.transaction;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.support.TestDatabaseConfig.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** O banco recusa, por SQL direto, dado de cartão inválido e pagamento de fatura que quebra o isolamento (V3). */
@SpringBootTest
class CardSchemaTest {

	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private TestDatabaseCleaner cleaner;

	private UUID userA;
	private UUID userB;

	@BeforeEach
	void prepara() {
		cleaner.truncateAll();
		userA = user("a@teste.com");
		userB = user("b@teste.com");
	}

	private UUID user(String email) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into users (id, email, password_hash) values (?, ?, 'hash')", id, email);
		return id;
	}

	private void account(UUID user, UUID id, String name, String type, BigDecimal limit, Integer closing, Integer due) {
		jdbc.update("insert into accounts (id, user_id, name, type, credit_limit, closing_day, due_day) "
				+ "values (?, ?, ?, ?, ?, ?, ?)", id, user, name, type, limit, closing, due);
	}

	@Test
	void cartaoValido_eContaComumSemCamposDeCartao_passam() {
		assertThatCode(() -> account(userA, UUID.randomUUID(), "Cartão", "CREDIT_CARD", new BigDecimal("1000.00"), 1, 28))
				.doesNotThrowAnyException();
		assertThatCode(() -> account(userA, UUID.randomUUID(), "Banco", "CHECKING", null, null, null))
				.doesNotThrowAnyException();
	}

	@Test
	void cartaoSemLimiteOuSemDia_ehRecusado() {
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "A", "CREDIT_CARD", null, 10, 20))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "B", "CREDIT_CARD", BigDecimal.TEN, null, 20))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "C", "CREDIT_CARD", BigDecimal.TEN, 10, null))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void diasForaDeUmAVinteEOito_eLimiteNegativo_saoRecusados() {
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "A", "CREDIT_CARD", BigDecimal.TEN, 0, 10))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "B", "CREDIT_CARD", BigDecimal.TEN, 10, 29))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "C", "CREDIT_CARD", new BigDecimal("-1"), 10, 10))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void camposDeCartaoEmContaComum_saoRecusados() {
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "A", "CHECKING", BigDecimal.TEN, null, null))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "B", "WALLET", null, 10, null))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> account(userA, UUID.randomUUID(), "C", "SAVINGS", null, null, 10))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void payment(UUID user, UUID card, UUID source, String month, String amount) {
		jdbc.update("insert into invoice_payments (user_id, card_account_id, source_account_id, invoice_month, amount, "
				+ "paid_on) values (?, ?, ?, ?, ?, ?)", user, card, source, LocalDate.parse(month),
				new BigDecimal(amount), LocalDate.of(2026, 12, 26));
	}

	@Test
	void pagamento_recusaValorZeroMesQueNaoEDiaUmEMesmaContaNosDoisLados() {
		UUID card = UUID.randomUUID();
		UUID bank = UUID.randomUUID();
		account(userA, card, "Cartão", "CREDIT_CARD", BigDecimal.TEN, 25, 5);
		account(userA, bank, "Banco", "CHECKING", null, null, null);

		assertThatCode(() -> payment(userA, card, bank, "2026-12-01", "10.00")).doesNotThrowAnyException();
		assertThatThrownBy(() -> payment(userA, card, bank, "2026-12-01", "0"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> payment(userA, card, bank, "2026-12-15", "10.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> payment(userA, card, card, "2026-12-01", "10.00"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void pagamento_recusaContaDeOutroUsuario_nosDoisLados() {
		UUID cardA = UUID.randomUUID();
		UUID bankA = UUID.randomUUID();
		UUID bankB = UUID.randomUUID();
		account(userA, cardA, "Cartão", "CREDIT_CARD", BigDecimal.TEN, 25, 5);
		account(userA, bankA, "Banco", "CHECKING", null, null, null);
		account(userB, bankB, "Banco B", "CHECKING", null, null, null);

		assertThatThrownBy(() -> payment(userA, cardA, bankB, "2026-12-01", "10.00")) // origem do B
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> payment(userB, cardA, bankB, "2026-12-01", "10.00")) // cartão do A, dono B
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
