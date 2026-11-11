package com.fintrack.transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.fintrack.shared.FlowType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionais da listagem. O filtro por usuário é sempre o primeiro e não é opcional. */
final class TransactionSpecifications {

	private TransactionSpecifications() {
	}

	static Specification<Transaction> filter(UUID userId, LocalDate from, LocalDate to,
			Collection<UUID> categoryIds, FlowType type, UUID accountId, String text) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			p.add(cb.equal(root.get("userId"), userId));
			if (from != null) {
				p.add(cb.greaterThanOrEqualTo(root.get("occurredOn"), from));
			}
			if (to != null) {
				p.add(cb.lessThanOrEqualTo(root.get("occurredOn"), to));
			}
			if (categoryIds != null) {
				p.add(root.get("categoryId").in(categoryIds));
			}
			if (type != null) {
				p.add(cb.equal(root.get("type"), type));
			}
			if (accountId != null) {
				p.add(cb.equal(root.get("accountId"), accountId));
			}
			if (text != null && !text.isBlank()) {
				String pattern = "%" + escapeLike(text.strip().toLowerCase()) + "%";
				p.add(cb.or(cb.like(cb.lower(root.get("merchant")), pattern, '\\'),
						cb.like(cb.lower(root.get("description")), pattern, '\\')));
			}
			return cb.and(p.toArray(Predicate[]::new));
		};
	}

	/** % e _ digitados pelo usuário valem como texto comum, não como curinga do LIKE. */
	static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
