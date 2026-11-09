package com.fintrack.auth;

import static com.fintrack.auth.AuthTestSupport.field;
import static com.fintrack.auth.AuthTestSupport.login;
import static com.fintrack.auth.AuthTestSupport.post;
import static com.fintrack.auth.AuthTestSupport.refresh;
import static com.fintrack.auth.AuthTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.fintrack.support.TestDatabaseConfig.TestDatabaseCleaner;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Nenhuma senha, token ou e-mail pode aparecer em log, nem em DEBUG, nem nos caminhos de erro (RNF-01). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoggingTest {

	private static final String EMAIL = "segredo.log@teste.com";
	private static final String SENHA = "SenhaMuitoSecreta-4711";
	private static final String SENHA_ERRADA = "SenhaErradaTambem-0815";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private TestDatabaseCleaner cleaner;

	@Test
	void fluxoCompleto_incluindoErros_naoVazaSenhaTokenNemEmailNoLog() throws Exception {
		cleaner.truncateAll();
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Logger app = (Logger) LoggerFactory.getLogger("com.fintrack");
		Logger sec = (Logger) LoggerFactory.getLogger("org.springframework.security");
		Level appLevel = app.getLevel();
		Level secLevel = sec.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		root.addAppender(appender);
		app.setLevel(Level.DEBUG);
		sec.setLevel(Level.DEBUG);
		List<String> segredos = new ArrayList<>(List.of(SENHA, SENHA_ERRADA, EMAIL));
		try {
			register(mvc, EMAIL, SENHA);
			register(mvc, EMAIL, SENHA); // duplicado
			login(mvc, EMAIL, SENHA_ERRADA); // falha
			var ok = login(mvc, EMAIL, SENHA);
			String access = field(ok, "$.accessToken");
			String r1 = field(ok, "$.refreshToken");
			String r2 = field(refresh(mvc, r1), "$.refreshToken");
			refresh(mvc, r1); // reuso: gera o log de alerta
			post(mvc, "/api/auth/logout", "refreshToken", r2);
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/_probe/me")
					.header("Authorization", "Bearer " + access));
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/_probe/me")
					.header("Authorization", "Bearer token.invalido.qualquer"));
			segredos.addAll(List.of(access, r1, r2));
		}
		finally {
			root.detachAppender(appender);
			app.setLevel(appLevel);
			sec.setLevel(secLevel);
		}

		assertThat(appender.list).isNotEmpty();
		for (ILoggingEvent event : appender.list) {
			String text = event.getFormattedMessage()
					+ (event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy()));
			for (String segredo : segredos) {
				assertThat(text).as("log vazou um segredo: %s", event.getLoggerName()).doesNotContain(segredo);
			}
		}
	}
}
