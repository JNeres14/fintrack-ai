package com.fintrack.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** Regras de datas da fatura (ADR-016), sem banco. */
class InvoiceCalendarTest {

	private static LocalDate d(String s) {
		return LocalDate.parse(s);
	}

	@Test
	void compraNoDiaDoFechamento_entraNaFaturaDoMes_eNoDiaSeguinteNaDoMesSeguinte() {
		InvoiceCalendar c = new InvoiceCalendar(25, 5);

		assertThat(c.invoiceOf(d("2026-12-25"))).isEqualTo(YearMonth.of(2026, 12));
		assertThat(c.invoiceOf(d("2026-12-26"))).isEqualTo(YearMonth.of(2027, 1)); // virada de ano
		assertThat(c.invoiceOf(d("2026-12-01"))).isEqualTo(YearMonth.of(2026, 12));
		assertThat(c.invoiceOf(d("2026-11-26"))).isEqualTo(YearMonth.of(2026, 12));
		assertThat(c.invoiceOf(d("2027-01-25"))).isEqualTo(YearMonth.of(2027, 1));
	}

	@Test
	void periodoVaiDoDiaSeguinteAoFechamentoAnteriorAteOFechamentoDoMes_inclusive() {
		InvoiceCalendar c = new InvoiceCalendar(25, 5);

		assertThat(c.periodStart(YearMonth.of(2026, 12))).isEqualTo(d("2026-11-26"));
		assertThat(c.closingDate(YearMonth.of(2026, 12))).isEqualTo(d("2026-12-25"));
		assertThat(c.periodStart(YearMonth.of(2027, 1))).isEqualTo(d("2026-12-26")); // atravessa o ano
	}

	@Test
	void vencimentoNoMesSeguinte_quandoODiaDeVencimentoEhMenorQueODeFechamento() {
		InvoiceCalendar c = new InvoiceCalendar(25, 5);

		assertThat(c.dueDate(YearMonth.of(2026, 12))).isEqualTo(d("2027-01-05"));
		assertThat(c.dueDate(YearMonth.of(2026, 11))).isEqualTo(d("2026-12-05"));
	}

	@Test
	void vencimentoNoMesmoMes_quandoODiaDeVencimentoEhMaiorQueODeFechamento() {
		InvoiceCalendar c = new InvoiceCalendar(5, 10);

		assertThat(c.dueDate(YearMonth.of(2026, 12))).isEqualTo(d("2026-12-10"));
		assertThat(c.periodStart(YearMonth.of(2026, 12))).isEqualTo(d("2026-11-06"));
		assertThat(c.closingDate(YearMonth.of(2026, 12))).isEqualTo(d("2026-12-05"));
	}

	@Test
	void vencimentoNoMesSeguinte_quandoOsDoisDiasSaoIguais() {
		InvoiceCalendar c = new InvoiceCalendar(10, 10);

		assertThat(c.dueDate(YearMonth.of(2026, 12))).isEqualTo(d("2027-01-10"));
	}

	@Test
	void diasExtremos_1e28_funcionamEmTodosOsMeses_inclusiveFevereiro() {
		InvoiceCalendar primeiro = new InvoiceCalendar(1, 28);
		assertThat(primeiro.closingDate(YearMonth.of(2027, 2))).isEqualTo(d("2027-02-01"));
		assertThat(primeiro.periodStart(YearMonth.of(2027, 3))).isEqualTo(d("2027-02-02"));
		assertThat(primeiro.dueDate(YearMonth.of(2027, 2))).isEqualTo(d("2027-02-28"));

		InvoiceCalendar ultimo = new InvoiceCalendar(28, 1);
		assertThat(ultimo.closingDate(YearMonth.of(2027, 2))).isEqualTo(d("2027-02-28"));
		assertThat(ultimo.periodStart(YearMonth.of(2027, 3))).isEqualTo(d("2027-03-01")); // 28/02 + 1 dia
		assertThat(ultimo.invoiceOf(d("2027-02-28"))).isEqualTo(YearMonth.of(2027, 2));
		assertThat(ultimo.invoiceOf(d("2027-03-01"))).isEqualTo(YearMonth.of(2027, 3));
		assertThat(ultimo.dueDate(YearMonth.of(2026, 12))).isEqualTo(d("2027-01-01"));
	}
}
