package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Toda consulta recebe o userId (vindo do token). Listagem com filtros e paginação entra no Bloco D. */
public interface TransactionRepository
		extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

	Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);

	boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);

	boolean existsByUserIdAndPaymentMethodId(UUID userId, UUID paymentMethodId);

	/** Receitas menos despesas por conta, somadas no banco (BigDecimal, sem arredondar). */
	interface AccountDelta {

		UUID getAccountId();

		BigDecimal getDelta();
	}

	@Query("""
			select t.accountId as accountId,
			       sum(case when t.type = com.fintrack.shared.FlowType.INCOME then t.amount else -t.amount end) as delta
			from Transaction t
			where t.userId = :userId
			group by t.accountId
			""")
	List<AccountDelta> sumDeltaByAccount(@Param("userId") UUID userId);

	@Query("""
			select coalesce(sum(case when t.type = com.fintrack.shared.FlowType.INCOME then t.amount else -t.amount end), 0)
			from Transaction t
			where t.userId = :userId and t.accountId = :accountId
			""")
	BigDecimal sumDeltaForAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);
}
