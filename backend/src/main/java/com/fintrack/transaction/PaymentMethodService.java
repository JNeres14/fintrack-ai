package com.fintrack.transaction;

import java.util.List;
import java.util.UUID;

import com.fintrack.shared.ConflictException;
import com.fintrack.shared.ResourceNotFoundException;
import com.fintrack.transaction.PaymentMethodDtos.PaymentMethodRequest;
import com.fintrack.transaction.PaymentMethodDtos.PaymentMethodResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentMethodService {

	private final PaymentMethodRepository methods;
	private final TransactionRepository transactions;

	PaymentMethodService(PaymentMethodRepository methods, TransactionRepository transactions) {
		this.methods = methods;
		this.transactions = transactions;
	}

	@Transactional(readOnly = true)
	List<PaymentMethodResponse> list(UUID userId) {
		return methods.findAllByUserIdOrderByName(userId).stream().map(PaymentMethodService::toResponse).toList();
	}

	@Transactional(readOnly = true)
	PaymentMethodResponse get(UUID userId, UUID id) {
		return toResponse(find(userId, id));
	}

	@Transactional
	PaymentMethodResponse create(UUID userId, PaymentMethodRequest req) {
		String name = req.name().strip();
		if (methods.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma forma de pagamento com este nome.");
		}
		return toResponse(methods.saveAndFlush(new PaymentMethod(userId, name)));
	}

	@Transactional
	PaymentMethodResponse update(UUID userId, UUID id, PaymentMethodRequest req) {
		PaymentMethod method = find(userId, id);
		String name = req.name().strip();
		if (!name.equalsIgnoreCase(method.getName()) && methods.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new ConflictException("Já existe uma forma de pagamento com este nome.");
		}
		method.setName(name);
		return toResponse(methods.saveAndFlush(method));
	}

	@Transactional
	void delete(UUID userId, UUID id) {
		PaymentMethod method = find(userId, id);
		if (transactions.existsByUserIdAndPaymentMethodId(userId, id)) {
			throw new ConflictException("A forma de pagamento tem transações e não pode ser excluída.");
		}
		methods.delete(method);
	}

	private PaymentMethod find(UUID userId, UUID id) {
		return methods.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento não encontrada."));
	}

	private static PaymentMethodResponse toResponse(PaymentMethod m) {
		return new PaymentMethodResponse(m.getId(), m.getName());
	}
}
