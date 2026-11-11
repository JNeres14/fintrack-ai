package com.fintrack.shared;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * "Hoje" no fuso do usuário (APP_TIMEZONE, padrão America/Sao_Paulo). Decide quando uma fatura fecha.
 * O resto do sistema segue em UTC; só a data de negócio usa este fuso. Classe não final para os testes poderem
 * trocar a data.
 */
@Component
public class AppToday {

	private final Clock clock;
	private final ZoneId zone;

	AppToday(Clock clock, @Value("${app.timezone}") String zone) {
		this.clock = clock;
		this.zone = ZoneId.of(zone);
	}

	public LocalDate today() {
		return LocalDate.now(clock.withZone(zone));
	}
}
