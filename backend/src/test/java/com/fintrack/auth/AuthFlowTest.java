package com.fintrack.auth;

import static com.fintrack.auth.AuthTestSupport.PASSWORD;
import static com.fintrack.auth.AuthTestSupport.field;
import static com.fintrack.auth.AuthTestSupport.login;
import static com.fintrack.auth.AuthTestSupport.post;
import static com.fintrack.auth.AuthTestSupport.refresh;
import static com.fintrack.auth.AuthTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import com.fintrack.support.TestDatabaseConfig.TestDatabaseCleaner;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Fluxo completo de autenticação contra o PostgreSQL de teste. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	private static final String EMAIL = "ana@teste.com";
	private static final String PROBE = "/api/_probe/me";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private JwtEncoder encoder;
	@Autowired
	private TestDatabaseCleaner cleaner;

	@BeforeEach
	void limpa() {
		cleaner.truncateAll();
	}

	// ---------- cadastro ----------

	@Test
	void cadastroFeliz_devolve201_guardaHashBcrypt_enaoDevolveSenha() throws Exception {
		register(mvc, EMAIL, PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))));

		String hash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, EMAIL);
		assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
	}

	@Test
	void emailRepetido_mesmoComMaiusculas_devolve409ComPerguntaDeSenha() throws Exception {
		register(mvc, EMAIL, PASSWORD).andExpect(status().isCreated());

		register(mvc, "ANA@Teste.com", PASSWORD)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um cadastro com este e-mail. Esqueceu a senha?"));
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
	}

	@Test
	void senhaFraca_curtaOuComum_devolve400() throws Exception {
		register(mvc, EMAIL, "curta123").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("password"));
		register(mvc, EMAIL, "123456789012").andExpect(status().isBadRequest());
		register(mvc, "anabeatriz@teste.com", "anabeatriz@teste.com").andExpect(status().isBadRequest()); // senha = e-mail
	}

	@Test
	void emailInvalido_devolve400() throws Exception {
		register(mvc, "isto-nao-e-email", PASSWORD).andExpect(status().isBadRequest());
	}

	// ---------- login ----------

	@Test
	void loginFeliz_devolveTokens_eTokenDeAcessoFuncionaNoEndpointProtegido() throws Exception {
		String userId = field(register(mvc, EMAIL, PASSWORD).andExpect(status().isCreated()), "$.id");

		var result = login(mvc, EMAIL, PASSWORD).andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(900));
		String access = field(result, "$.accessToken");

		mvc.perform(get(PROBE).header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(content().string(userId)); // o CurrentUser devolve o id do token, não do cliente
	}

	@Test
	void loginComEmailEmOutraCaixa_funciona() throws Exception {
		register(mvc, EMAIL, PASSWORD).andExpect(status().isCreated());
		login(mvc, "ANA@TESTE.COM", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void senhaErrada_eEmailInexistente_dao401IdenticoSemRevelarQualFoi() throws Exception {
		register(mvc, EMAIL, PASSWORD).andExpect(status().isCreated());

		String errada = login(mvc, EMAIL, "OutraSenha-12345").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String inexistente = login(mvc, "ninguem@teste.com", "OutraSenha-12345").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(errada).isEqualTo(inexistente);
	}

	// ---------- token de acesso ----------

	@Test
	void endpointProtegidoSemToken_devolve401() throws Exception {
		mvc.perform(get(PROBE)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/qualquer-coisa")).andExpect(status().isUnauthorized());
		mvc.perform(get(PROBE).header("Authorization", "Bearer lixo")).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenExpirado_devolve401() throws Exception {
		String userId = field(register(mvc, EMAIL, PASSWORD), "$.id");
		String expired = token(encoder, userId, Instant.now().minusSeconds(1));

		mvc.perform(get(PROBE).header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
		// controle: o mesmo tipo de token, ainda válido, passa
		String valid = token(encoder, userId, Instant.now().plusSeconds(60));
		mvc.perform(get(PROBE).header("Authorization", "Bearer " + valid)).andExpect(status().isOk());
	}

	@Test
	void tokenAssinadoComOutraChave_ouSemAssinatura_devolve401() throws Exception {
		String userId = field(register(mvc, EMAIL, PASSWORD), "$.id");

		JwtEncoder other = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
				"outra-chave-qualquer-com-mais-de-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		mvc.perform(get(PROBE).header("Authorization", "Bearer " + token(other, userId, Instant.now().plusSeconds(60))))
				.andExpect(status().isUnauthorized());

		String header = b64("{\"alg\":\"none\"}");
		String payload = b64("{\"sub\":\"" + userId + "\",\"iss\":\"fintrack-ai\",\"exp\":9999999999}");
		mvc.perform(get(PROBE).header("Authorization", "Bearer " + header + "." + payload + "."))
				.andExpect(status().isUnauthorized());
	}

	// ---------- refresh ----------

	@Test
	void refresh_giraOToken_eOAntigoNaoServeMais() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		String r1 = field(login(mvc, EMAIL, PASSWORD), "$.refreshToken");

		var second = refresh(mvc, r1).andExpect(status().isOk());
		String r2 = field(second, "$.refreshToken");
		assertThat(r2).isNotEqualTo(r1);
		mvc.perform(get(PROBE).header("Authorization", "Bearer " + field(second, "$.accessToken")))
				.andExpect(status().isOk());
	}

	@Test
	void refreshReutilizado_devolve401_eRevogaAFamiliaInteira() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		String r1 = field(login(mvc, EMAIL, PASSWORD), "$.refreshToken");
		String r2 = field(refresh(mvc, r1).andExpect(status().isOk()), "$.refreshToken");

		refresh(mvc, r1).andExpect(status().isUnauthorized()); // reuso do token já gasto

		// O token novo (r2) pertencia à mesma família e também foi revogado.
		refresh(mvc, r2).andExpect(status().isUnauthorized());
		assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where revoked_at is null", Integer.class))
				.isZero();
	}

	@Test
	void refreshInventado_ouExpirado_devolve401() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		refresh(mvc, "token-que-nunca-existiu").andExpect(status().isUnauthorized());

		String r1 = field(login(mvc, EMAIL, PASSWORD), "$.refreshToken");
		jdbc.update("update refresh_tokens set expires_at = now() - interval '1 minute'");
		refresh(mvc, r1).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshTokenNoBancoEhSoHash_nuncaOValorOriginal() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		String raw = field(login(mvc, EMAIL, PASSWORD), "$.refreshToken");

		assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class, raw))
				.isZero();
		assertThat(jdbc.queryForObject("select length(token_hash) from refresh_tokens", Integer.class)).isEqualTo(64);
	}

	@Test
	void logout_revogaORefresh_eEhIdempotente() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		String r1 = field(login(mvc, EMAIL, PASSWORD), "$.refreshToken");

		post(mvc, "/api/auth/logout", "refreshToken", r1).andExpect(status().isNoContent());
		refresh(mvc, r1).andExpect(status().isUnauthorized());
		post(mvc, "/api/auth/logout", "refreshToken", r1).andExpect(status().isNoContent());
		post(mvc, "/api/auth/logout", "refreshToken", "desconhecido").andExpect(status().isNoContent());
	}

	// ---------- bloqueio por tentativas ----------

	@Test
	void cincoFalhasBloqueiam_senhaCertaNaoEntraEnquantoBloqueado_edepoisDoPrazoVolta() throws Exception {
		register(mvc, EMAIL, PASSWORD);

		for (int i = 0; i < 4; i++) {
			login(mvc, EMAIL, "Errada-Errada-123").andExpect(status().isUnauthorized());
		}
		assertThat(jdbc.queryForObject("select locked_until is null from users", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select failed_attempts from users", Integer.class)).isEqualTo(4);

		login(mvc, EMAIL, "Errada-Errada-123").andExpect(status().isUnauthorized()); // 5ª falha: bloqueia
		assertThat(jdbc.queryForObject("select locked_until > now() from users", Boolean.class)).isTrue();

		String bloqueado = login(mvc, EMAIL, PASSWORD).andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		assertThat(bloqueado).doesNotContainIgnoringCase("bloque"); // resposta igual à de senha errada

		jdbc.update("update users set locked_until = now() - interval '1 minute'"); // simula o fim dos 15 min
		login(mvc, EMAIL, PASSWORD).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select failed_attempts from users", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select locked_until is null from users", Boolean.class)).isTrue();
	}

	@Test
	void loginCertoZeraOContadorDeFalhas() throws Exception {
		register(mvc, EMAIL, PASSWORD);
		for (int i = 0; i < 3; i++) {
			login(mvc, EMAIL, "Errada-Errada-123").andExpect(status().isUnauthorized());
		}
		login(mvc, EMAIL, PASSWORD).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select failed_attempts from users", Integer.class)).isZero();
	}

	// ---------- rotas abertas e CORS ----------

	@Test
	void healthFicaAbertoSemToken() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
	}

	@Test
	void cors_liberaSoAOrigemConfigurada() throws Exception {
		mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mvc.perform(options("/api/auth/login").header("Origin", "http://site-malicioso.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	// ---------- utilitários ----------

	private static String token(JwtEncoder enc, String userId, Instant exp) {
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fintrack-ai").subject(userId)
				.issuedAt(exp.minusSeconds(900)).expiresAt(exp).build();
		return enc.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private static String b64(String s) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}
}
