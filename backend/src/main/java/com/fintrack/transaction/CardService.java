package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fintrack.shared.AppToday;
import com.fintrack.shared.BusinessRuleException;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.CardDtos.CardResponse;
import com.fintrack.transaction.CardDtos.InvoiceDetail;
import com.fintrack.transaction.CardDtos.InvoiceStatus;
import com.fintrack.transaction.CardDtos.InvoiceSummary;
import com.fintrack.transaction.CardDtos.PaymentRequest;
import com.fintrack.transaction.CardDtos.PaymentResponse;
import com.fintrack.transaction.TransactionDtos.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartão, fatura e pagamento de fatura (ADR-016). A fatura é calculada a cada consulta a partir das transações do
 * cartão; só o pagamento é gravado. Toda operação recebe o userId do token; cartão de outro usuário = 404.
 * Logs: só ids e tipo do evento, nunca valores.
 */
@Service
class CardService {

	private static final Logger log = LoggerFactory.getLogger(CardService.class);
	private static final YearMonth MIN_MONTH = YearMonth.of(2000, 1);

	private final AccountRepository accounts;
	private final TransactionRepository transactions;
	private final InvoicePaymentRepository payments;
	private final AccountBalances balances;
	private final AppToday today;

	CardService(AccountRepository accounts, TransactionRepository transactions, InvoicePaymentRepository payments,
			AccountBalances balances, AppToday today) {
		this.accounts = accounts;
		this.transactions = transactions;
		this.payments = payments;
		this.balances = balances;
		this.today = today;
	}

	@Transactional(readOnly = true)
	List<CardResponse> list(UUID userId) {
		LocalDate now = today.today();
		Map<UUID, BigDecimal> deltas = balances.deltasByAccount(userId);
		return accounts.findAllByUserIdAndTypeOrderByName(userId, AccountType.CREDIT_CARD).stream().map(card -> {
			BigDecimal balance = balances.balance(card, deltas);
			BigDecimal used = balance.signum() < 0 ? balance.negate() : AccountBalances.ZERO;
			BigDecimal available = AccountBalances.scale(card.getCreditLimit().subtract(used));
			InvoiceCalendar calendar = calendar(card);
			YearMonth current = calendar.invoiceOf(now);
			return new CardResponse(card.getId(), card.getName(), card.getCreditLimit(), used, available, balance,
					card.getClosingDay(), card.getDueDay(), summarize(userId, card, calendar, current, now),
					summarize(userId, card, calendar, current.minusMonths(1), now));
		}).toList();
	}

	@Transactional(readOnly = true)
	InvoiceDetail invoice(UUID userId, UUID cardId, String monthText) {
		Account card = findCard(userId, cardId);
		LocalDate now = today.today();
		YearMonth month = parseMonth(monthText, now);
		InvoiceCalendar calendar = calendar(card);
		InvoiceSummary summary = summarize(userId, card, calendar, month, now);
		List<TransactionResponse> purchases = transactions
				.findAllByUserIdAndAccountIdAndOccurredOnBetween(userId, cardId, summary.periodStart(),
						summary.closingDate(), Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt"),
								Sort.Order.desc("id")))
				.stream().map(TransactionResponse::from).toList();
		List<PaymentResponse> paid = payments
				.findAllByUserIdAndCardAccountIdAndInvoiceMonthOrderByPaidOnAscCreatedAtAsc(userId, cardId,
						month.atDay(1))
				.stream().map(CardService::toResponse).toList();
		return new InvoiceDetail(summary, purchases, paid);
	}

	@Transactional
	PaymentResponse pay(UUID userId, UUID cardId, String monthText, PaymentRequest req) {
		findCard(userId, cardId);
		// Trava o cartão: pagamento e alteração de compra da mesma fatura não acontecem ao mesmo tempo.
		Account card = accounts.findByIdAndUserIdForUpdate(cardId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado."));
		LocalDate now = today.today();
		YearMonth month = parseMonth(monthText, now);

		Account source = accounts.findByIdAndUserId(req.sourceAccountId(), userId)
				.orElseThrow(() -> new ResourceNotFoundException("Conta de origem não encontrada."));
		if (source.isCreditCard()) {
			throw new BusinessRuleException("A conta de origem do pagamento não pode ser um cartão de crédito.");
		}

		InvoiceSummary summary = summarize(userId, card, calendar(card), month, now);
		if (summary.status() == InvoiceStatus.OPEN) {
			throw new BusinessRuleException(
					"A fatura ainda está aberta. O pagamento só é possível depois do fechamento.");
		}
		if (summary.total().signum() <= 0) {
			throw new BusinessRuleException("A fatura não tem valor a pagar.");
		}
		if (req.amount().compareTo(summary.remaining()) > 0) {
			throw new BusinessRuleException("O valor é maior que o saldo restante da fatura.");
		}
		LocalDate paidOn = req.paidOn() == null ? now : req.paidOn();
		if (paidOn.isBefore(summary.closingDate()) || paidOn.isAfter(now)) {
			throw new BusinessRuleException(
					"A data do pagamento deve estar entre o fechamento da fatura e hoje.");
		}

		InvoicePayment saved = payments.saveAndFlush(new InvoicePayment(userId, cardId, source.getId(),
				month.atDay(1), req.amount().setScale(2), paidOn));
		log.info("Pagamento de fatura registrado: userId={}, paymentId={}", userId, saved.getId());
		return toResponse(saved);
	}

	@Transactional
	void deletePayment(UUID userId, UUID cardId, String monthText, UUID paymentId) {
		findCard(userId, cardId);
		accounts.findByIdAndUserIdForUpdate(cardId, userId);
		YearMonth month = parseMonth(monthText, today.today());
		InvoicePayment payment = payments
				.findByIdAndUserIdAndCardAccountIdAndInvoiceMonth(paymentId, userId, cardId, month.atDay(1))
				.orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));
		payments.delete(payment);
		log.info("Pagamento de fatura excluído: userId={}, paymentId={}", userId, paymentId);
	}

	/** Cartão de outro usuário, inexistente ou conta que não é cartão: tudo igual, 404. */
	private Account findCard(UUID userId, UUID cardId) {
		return accounts.findByIdAndUserId(cardId, userId).filter(Account::isCreditCard)
				.orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado."));
	}

	private InvoiceSummary summarize(UUID userId, Account card, InvoiceCalendar calendar, YearMonth month,
			LocalDate now) {
		LocalDate start = calendar.periodStart(month);
		LocalDate closing = calendar.closingDate(month);
		// total = despesas - estornos = o oposto da variação da conta no período
		BigDecimal total = AccountBalances
				.scale(transactions.sumDeltaForAccountBetween(userId, card.getId(), start, closing).negate());
		BigDecimal paid = AccountBalances.scale(payments.sumForInvoice(userId, card.getId(), month.atDay(1)));
		BigDecimal remaining = total.subtract(paid).max(AccountBalances.ZERO);
		InvoiceStatus status = !now.isAfter(closing) ? InvoiceStatus.OPEN
				: paid.compareTo(total) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.CLOSED;
		return new InvoiceSummary(month.toString(), start, closing, calendar.dueDate(month), status, total, paid,
				remaining);
	}

	private static InvoiceCalendar calendar(Account card) {
		return new InvoiceCalendar(card.getClosingDay(), card.getDueDay());
	}

	private static YearMonth parseMonth(String text, LocalDate now) {
		YearMonth month;
		try {
			month = YearMonth.parse(text);
		}
		catch (DateTimeParseException e) {
			throw new BusinessRuleException("Mês inválido: use o formato AAAA-MM.");
		}
		if (month.isBefore(MIN_MONTH) || month.isAfter(YearMonth.from(now).plusYears(1).plusMonths(1))) {
			throw new BusinessRuleException("Mês fora do intervalo permitido.");
		}
		return month;
	}

	private static PaymentResponse toResponse(InvoicePayment p) {
		return new PaymentResponse(p.getId(), p.getCardAccountId(), p.getSourceAccountId(),
				YearMonth.from(p.getInvoiceMonth()).toString(), p.getAmount(), p.getPaidOn());
	}
}
