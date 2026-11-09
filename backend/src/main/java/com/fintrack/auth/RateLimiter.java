package com.fintrack.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Rate limiting simples em memória (ADR-013): janela fixa de 1 minuto por chave (endpoint + IP).
 * Limitação aceita para uso pessoal: não é compartilhado entre instâncias e zera ao reiniciar.
 */
@Component
class RateLimiter {

	private static final Duration WINDOW = Duration.ofMinutes(1);
	private static final int PURGE_THRESHOLD = 10_000;

	private record Window(Instant start, int count) {
	}

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
	private final int limit;
	private final Clock clock;

	RateLimiter(AuthProperties props, Clock clock) {
		this.limit = props.rateLimitPerMinute();
		this.clock = clock;
	}

	/** Conta o pedido; lança TooManyRequestsException se passou do limite da janela atual. */
	void check(String key) {
		Instant now = clock.instant();
		if (windows.size() > PURGE_THRESHOLD) {
			windows.values().removeIf(w -> !w.start().plus(WINDOW).isAfter(now));
		}
		Window w = windows.compute(key, (k, old) -> old == null || !old.start().plus(WINDOW).isAfter(now)
				? new Window(now, 1) : new Window(old.start(), old.count() + 1));
		if (w.count() > limit) {
			long retry = Math.max(1, Duration.between(now, w.start().plus(WINDOW)).toSeconds());
			throw new TooManyRequestsException(retry);
		}
	}
}
