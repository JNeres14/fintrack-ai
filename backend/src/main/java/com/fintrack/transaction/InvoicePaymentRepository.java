package com.fintrack.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Toda consulta recebe o userId (vindo do token). */
interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {

	interface AccountSum {

		UUID getAccountId();

		BigDecimal getTotal();
	}

	Optional<InvoicePayment> findByIdAndUserIdAndCardAccountIdAndInvoiceMonth(UUID id, UUID userId, UUID cardAccountId,
			LocalDate invoiceMonth);

	List<InvoicePayment> findAllByUserIdAndCardAccountIdAndInvoiceMonthOrderByPaidOnAscCreatedAtAsc(UUID userId,
			UUID cardAccountId, LocalDate invoiceMonth);

	boolean existsByUserIdAndCardAccountIdAndInvoiceMonth(UUID userId, UUID cardAccountId, LocalDate invoiceMonth);

	boolean existsByUserIdAndCardAccountId(UUID userId, UUID cardAccountId);

	boolean existsByUserIdAndSourceAccountId(UUID userId, UUID sourceAccountId);

	@Query("select coalesce(sum(p.amount), 0) from InvoicePayment p "
			+ "where p.userId = :userId and p.cardAccountId = :cardId and p.invoiceMonth = :month")
	BigDecimal sumForInvoice(@Param("userId") UUID userId, @Param("cardId") UUID cardId,
			@Param("month") LocalDate month);

	/** Pagamentos recebidos pelo cartão (reduzem a dívida). */
	@Query("select p.cardAccountId as accountId, sum(p.amount) as total from InvoicePayment p "
			+ "where p.userId = :userId group by p.cardAccountId")
	List<AccountSum> sumReceivedByCard(@Param("userId") UUID userId);

	/** Pagamentos feitos a partir da conta de origem (reduzem o saldo dela). */
	@Query("select p.sourceAccountId as accountId, sum(p.amount) as total from InvoicePayment p "
			+ "where p.userId = :userId group by p.sourceAccountId")
	List<AccountSum> sumPaidBySource(@Param("userId") UUID userId);
}
