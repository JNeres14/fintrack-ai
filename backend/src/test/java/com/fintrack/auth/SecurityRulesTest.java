package com.fintrack.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fintrack.shared.CorsProperties;
import org.junit.jupiter.api.Test;

/** Regras que não precisam de banco: política de senha e validação da configuração de segurança. */
class SecurityRulesTest {

	private static final String SEGREDO_OK = "0123456789abcdef0123456789abcdef"; // 32 bytes

	private final PasswordPolicy policy = new PasswordPolicy();

	@Test
	void senha_minimo12Caracteres() {
		assertThat(policy.validate("onze-chars1", "a@b.com")).isPresent();
		assertThat(policy.validate("doze-chars12", "a@b.com")).isEmpty();
	}

	@Test
	void senha_maximo72Bytes_emVezDeTruncarSilenciosamente() {
		assertThat(policy.validate("a1".repeat(36), "a@b.com")).isEmpty(); // 72 bytes
		assertThat(policy.validate("a1".repeat(36) + "x", "a@b.com")).isPresent(); // 73 bytes
		assertThat(policy.validate("ç".repeat(37), "a@b.com")).isPresent(); // 37 caracteres, 74 bytes
	}

	@Test
	void senha_recusaComumEIgualAoEmail() {
		assertThat(policy.validate("123456789012", "a@b.com")).isPresent();
		assertThat(policy.validate("PASSWORD1234", "a@b.com")).isPresent();
		assertThat(policy.validate("pessoa@exemplo.com", "Pessoa@Exemplo.com")).isPresent();
	}

	@Test
	void segredoDoJwt_ausenteOuCurto_naoSobe_eAMensagemNaoTrazOSegredo() {
		assertThatThrownBy(() -> new JwtProperties(null, 15, 7)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtProperties("", 15, 7)).isInstanceOf(IllegalStateException.class);
		String curto = "segredo-curto-123";
		assertThatThrownBy(() -> new JwtProperties(curto, 15, 7)).isInstanceOf(IllegalStateException.class)
				.message().doesNotContain(curto);
		assertThat(new JwtProperties(SEGREDO_OK, 15, 7)).isNotNull();
	}

	@Test
	void expiracaoDoJwt_nuncaPassaDe15Minutos() {
		assertThatThrownBy(() -> new JwtProperties(SEGREDO_OK, 16, 7)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtProperties(SEGREDO_OK, 0, 7)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void jwtProperties_toStringNaoMostraOSegredo() {
		assertThat(new JwtProperties(SEGREDO_OK, 15, 7).toString()).doesNotContain(SEGREDO_OK);
	}

	@Test
	void cors_recusaCuringa_eVazioSignificaNenhumaOrigem() {
		assertThatThrownBy(() -> new CorsProperties(java.util.List.of("*"))).isInstanceOf(IllegalStateException.class);
		assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
		assertThat(new CorsProperties(java.util.List.of("", " ")).allowedOrigins()).isEmpty();
		assertThat(new CorsProperties(java.util.List.of(" http://localhost:5173 ")).allowedOrigins())
				.containsExactly("http://localhost:5173");
	}
}
