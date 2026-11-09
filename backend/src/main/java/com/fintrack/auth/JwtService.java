package com.fintrack.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o JWT de acesso. O token leva só o userId (sub): nada de e-mail, valores ou dados financeiros. */
@Service
class JwtService {

	private final JwtEncoder encoder;
	private final JwtProperties props;
	private final Clock clock;

	JwtService(JwtEncoder encoder, JwtProperties props, Clock clock) {
		this.encoder = encoder;
		this.props = props;
		this.clock = clock;
	}

	Duration accessTtl() {
		return Duration.ofMinutes(props.accessTtlMinutes());
	}

	String issueAccessToken(UUID userId) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(AuthConfig.ISSUER)
				.subject(userId.toString())
				.issuedAt(now)
				.expiresAt(now.plus(accessTtl()))
				.id(UUID.randomUUID().toString())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
