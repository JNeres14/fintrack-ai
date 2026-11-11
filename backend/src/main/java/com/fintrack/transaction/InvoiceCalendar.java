package com.fintrack.transaction;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Regras de datas da fatura (ADR-016). Lógica pura, sem banco. Dias de 1 a 28, então todo mês tem o dia.
 * O nome da fatura (YearMonth) é o mês do FECHAMENTO.
 */
record InvoiceCalendar(int closingDay, int dueDay) {

	/** Compra até o dia de fechamento (inclusive) entra na fatura do mês; depois, na do mês seguinte. */
	YearMonth invoiceOf(LocalDate purchaseDate) {
		YearMonth month = YearMonth.from(purchaseDate);
		return purchaseDate.getDayOfMonth() <= closingDay ? month : month.plusMonths(1);
	}

	LocalDate closingDate(YearMonth invoice) {
		return invoice.atDay(closingDay);
	}

	/** Dia seguinte ao fechamento da fatura anterior. */
	LocalDate periodStart(YearMonth invoice) {
		return closingDate(invoice.minusMonths(1)).plusDays(1);
	}

	/** Mesmo mês do fechamento se due_day > closing_day; no mês seguinte se due_day <= closing_day. */
	LocalDate dueDate(YearMonth invoice) {
		YearMonth dueMonth = dueDay > closingDay ? invoice : invoice.plusMonths(1);
		return dueMonth.atDay(dueDay);
	}
}
