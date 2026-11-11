package com.fintrack.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Toda consulta recebe o userId (vindo do token, nunca do cliente). */
public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByIdAndUserId(UUID id, UUID userId);

	List<Account> findAllByUserIdOrderByName(UUID userId);

	List<Account> findAllByUserIdAndTypeOrderByName(UUID userId, AccountType type);

	boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

	/**
	 * Trava a linha da conta até o fim da transação. Pagamento de fatura e alteração de compra do cartão passam
	 * por aqui, então não acontecem ao mesmo tempo sobre a mesma fatura (ADR-016).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id and a.userId = :userId")
	Optional<Account> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
