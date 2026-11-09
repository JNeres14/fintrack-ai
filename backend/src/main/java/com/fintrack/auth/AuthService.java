package com.fintrack.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.fintrack.auth.AuthDtos.TokenResponse;
import com.fintrack.user.UserService;
import com.fintrack.user.UserService.UserView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de cadastro, login, refresh e logout. Decisões e justificativas: ADR-010 a ADR-014.
 * Logs: só tipo do evento e userId; nunca senha, token nem e-mail.
 */
@Service
class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserService users;
	private final RefreshTokenRepository refreshTokens;
	private final JwtService jwt;
	private final JwtProperties jwtProps;
	private final AuthProperties props;
	private final PasswordEncoder encoder;
	private final PasswordPolicy policy;
	private final Clock clock;
	/** Hash de uma senha qualquer: comparado quando o e-mail não existe, para o tempo de resposta não revelar isso. */
	private final String dummyHash;

	AuthService(UserService users, RefreshTokenRepository refreshTokens, JwtService jwt, JwtProperties jwtProps,
			AuthProperties props, PasswordEncoder encoder, PasswordPolicy policy, Clock clock) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.jwt = jwt;
		this.jwtProps = jwtProps;
		this.props = props;
		this.encoder = encoder;
		this.policy = policy;
		this.clock = clock;
		this.dummyHash = encoder.encode(UUID.randomUUID().toString());
	}

	UserView register(String email, String password) {
		policy.validate(password, email).ifPresent(msg -> {
			throw new InvalidPasswordException(msg);
		});
		if (users.emailExists(email)) {
			throw new EmailAlreadyRegisteredException();
		}
		try {
			UserView created = users.create(email, encoder.encode(password));
			log.info("Usuário cadastrado: userId={}", created.id());
			return created;
		}
		catch (DataIntegrityViolationException e) {
			// Dois cadastros simultâneos do mesmo e-mail: a restrição UNIQUE do banco decide.
			throw new EmailAlreadyRegisteredException();
		}
	}

	/** Sem @Transactional de propósito: o contador de falhas precisa ser gravado mesmo quando a resposta é 401. */
	TokenResponse login(String email, String password) {
		Instant now = clock.instant();
		Optional<UserView> found = users.findByEmail(email);
		if (found.isEmpty()) {
			encoder.matches(password, dummyHash);
			throw new AuthenticationFailedException();
		}
		UserView user = found.get();
		if (user.isLocked(now)) {
			// Conta bloqueada: nem confere a senha e responde igual a "senha errada".
			log.warn("Login recusado: conta bloqueada, userId={}", user.id());
			throw new AuthenticationFailedException();
		}
		if (!encoder.matches(password, user.passwordHash())) {
			users.registerFailedLogin(user.id(), props.maxFailedAttempts(), Duration.ofMinutes(props.lockMinutes()),
					now);
			log.warn("Login recusado: credencial inválida, userId={}", user.id());
			throw new AuthenticationFailedException();
		}
		users.clearFailedLogins(user.id());
		return issueTokens(user.id(), UUID.randomUUID());
	}

	/** noRollbackFor: ao detectar reuso, a revogação da família precisa ser gravada mesmo respondendo 401. */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	TokenResponse refresh(String rawToken) {
		Instant now = clock.instant();
		RefreshToken token = refreshTokens.findByTokenHash(hash(rawToken))
				.orElseThrow(AuthenticationFailedException::new);
		if (token.getRevokedAt() != null) {
			throw new AuthenticationFailedException();
		}
		if (token.getUsedAt() != null) {
			throw reuseDetected(token, now);
		}
		if (!token.getExpiresAt().isAfter(now)) {
			throw new AuthenticationFailedException();
		}
		if (refreshTokens.markUsed(token.getId(), now) == 0) {
			throw reuseDetected(token, now);
		}
		return issueTokens(token.getUserId(), token.getFamilyId());
	}

	private AuthenticationFailedException reuseDetected(RefreshToken token, Instant now) {
		refreshTokens.revokeFamily(token.getFamilyId(), now);
		log.warn("Refresh token já usado reapresentado; família revogada, userId={}", token.getUserId());
		return new AuthenticationFailedException();
	}

	/** Idempotente e sem informação: token desconhecido também responde sucesso. */
	@Transactional
	void logout(String rawToken) {
		refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(t -> {
			refreshTokens.revokeFamily(t.getFamilyId(), clock.instant());
			log.info("Logout: userId={}", t.getUserId());
		});
	}

	private TokenResponse issueTokens(UUID userId, UUID familyId) {
		byte[] bytes = new byte[32]; // 256 bits aleatórios
		RANDOM.nextBytes(bytes);
		String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		refreshTokens.save(new RefreshToken(userId, familyId, hash(rawRefresh),
				clock.instant().plus(Duration.ofDays(jwtProps.refreshTtlDays()))));
		return new TokenResponse("Bearer", jwt.issueAccessToken(userId), jwt.accessTtl().toSeconds(), rawRefresh);
	}

	private static String hash(String value) {
		try {
			return HexFormat.of()
					.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
