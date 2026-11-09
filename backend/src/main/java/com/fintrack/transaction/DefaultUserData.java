package com.fintrack.transaction;

import java.math.BigDecimal;
import java.util.List;

import com.fintrack.user.UserRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Dados padrão de cada novo usuário: formas de pagamento (Pix, débito, crédito, dinheiro) e uma conta "Carteira",
 * porque toda transação exige conta. Roda dentro da transação do cadastro.
 */
@Component
class DefaultUserData {

	static final List<String> PAYMENT_METHODS = List.of("Pix", "Débito", "Crédito", "Dinheiro");
	static final String DEFAULT_ACCOUNT = "Carteira";

	private final PaymentMethodRepository methods;
	private final AccountRepository accounts;

	DefaultUserData(PaymentMethodRepository methods, AccountRepository accounts) {
		this.methods = methods;
		this.accounts = accounts;
	}

	@EventListener
	void onUserRegistered(UserRegistered event) {
		PAYMENT_METHODS.forEach(name -> methods.save(new PaymentMethod(event.userId(), name)));
		accounts.save(new Account(event.userId(), DEFAULT_ACCOUNT, AccountType.WALLET,
				BigDecimal.ZERO.setScale(2)));
	}
}
