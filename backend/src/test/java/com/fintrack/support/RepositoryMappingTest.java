package com.fintrack.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fintrack.budget.Budget;
import com.fintrack.budget.BudgetRepository;
import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.shared.FlowType;
import com.fintrack.transaction.Account;
import com.fintrack.transaction.AccountRepository;
import com.fintrack.transaction.AccountType;
import com.fintrack.transaction.PaymentMethod;
import com.fintrack.transaction.PaymentMethodRepository;
import com.fintrack.transaction.Transaction;
import com.fintrack.transaction.TransactionRepository;
import com.fintrack.user.User;
import com.fintrack.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Entidades e repositories contra o PostgreSQL real: mapeamento, precisão do dinheiro e filtro por usuário. */
@SpringBootTest
@Transactional
class RepositoryMappingTest {

	@Autowired
	private UserRepository users;
	@Autowired
	private AccountRepository accounts;
	@Autowired
	private CategoryRepository categories;
	@Autowired
	private PaymentMethodRepository paymentMethods;
	@Autowired
	private TransactionRepository transactions;
	@Autowired
	private BudgetRepository budgets;
	@Autowired
	private EntityManager em;

	@Test
	void persisteEReleTransacaoMantendoPrecisaoEAuditoria() {
		User user = users.save(new User("  Pessoa@Teste.COM ", "hash-de-teste"));
		Account account = accounts.save(new Account(user.getId(), "Carteira", AccountType.WALLET, new BigDecimal("1234.50")));
		Category category = categories.save(new Category(user.getId(), "Alimentação", FlowType.EXPENSE, null));
		PaymentMethod pix = paymentMethods.save(new PaymentMethod(user.getId(), "Pix"));
		Transaction saved = transactions.save(new Transaction(user.getId(), account.getId(), category.getId(),
				pix.getId(), FlowType.EXPENSE, new BigDecimal("10.50"), LocalDate.of(2026, 11, 7), "Mercado", "Compras"));
		em.flush();
		em.clear();

		Transaction found = transactions.findByIdAndUserId(saved.getId(), user.getId()).orElseThrow();
		assertThat(found.getAmount()).isEqualByComparingTo("10.50");
		assertThat(found.getAmount().scale()).isEqualTo(2);
		assertThat(found.getOccurredOn()).isEqualTo(LocalDate.of(2026, 11, 7));
		assertThat(found.getType()).isEqualTo(FlowType.EXPENSE);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
		assertThat(accounts.findByIdAndUserId(account.getId(), user.getId()).orElseThrow().getInitialBalance())
				.isEqualByComparingTo("1234.50");
		assertThat(users.findByEmail("pessoa@teste.com")).isPresent();
	}

	@Test
	void usuarioNaoEnxergaDadosDeOutroPeloRepository() {
		User a = users.save(new User("a@teste.com", "h"));
		User b = users.save(new User("b@teste.com", "h"));
		Account account = accounts.save(new Account(a.getId(), "Conta do A", AccountType.CHECKING, BigDecimal.ZERO));
		Category category = categories.save(new Category(a.getId(), "Moradia", FlowType.EXPENSE, null));
		Transaction tx = transactions.save(new Transaction(a.getId(), account.getId(), category.getId(), null,
				FlowType.EXPENSE, new BigDecimal("99.99"), LocalDate.of(2026, 11, 1), null, null));
		em.flush();

		assertThat(accounts.findByIdAndUserId(account.getId(), b.getId())).isEmpty();
		assertThat(categories.findByIdAndUserId(category.getId(), b.getId())).isEmpty();
		assertThat(transactions.findByIdAndUserId(tx.getId(), b.getId())).isEmpty();
		assertThat(accounts.findAllByUserIdOrderByName(b.getId())).isEmpty();
		assertThat(accounts.findAllByUserIdOrderByName(a.getId())).hasSize(1);
	}

	@Test
	void orcamentoNormalizaParaODiaUmEPermiteCategoriaNula() {
		User user = users.save(new User("c@teste.com", "h"));
		UUID none = null;
		Budget saved = budgets.save(new Budget(user.getId(), none, new BigDecimal("1500.00"), LocalDate.of(2026, 11, 20)));
		em.flush();
		em.clear();

		Budget found = budgets.findByIdAndUserId(saved.getId(), user.getId()).orElseThrow();
		assertThat(found.getMonth()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(found.getCategoryId()).isNull();
		assertThat(budgets.findAllByUserIdAndMonth(user.getId(), LocalDate.of(2026, 11, 1))).hasSize(1);
	}
}
