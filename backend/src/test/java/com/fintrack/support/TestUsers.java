package com.fintrack.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Cria usuários reais pela API (cadastro + login) e devolve o id e o token de acesso, para os testes dos módulos. */
public final class TestUsers {

	public static final String PASSWORD = "SenhaDeTeste-2026!";

	public record Session(UUID userId, String token) {

		public String bearer() {
			return "Bearer " + token;
		}
	}

	private TestUsers() {
	}

	public static Session create(MockMvc mvc, String email) throws Exception {
		String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
		String reg = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse().getContentAsString();
		String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse().getContentAsString();
		return new Session(UUID.fromString(JsonPath.read(reg, "$.id")), JsonPath.read(login, "$.accessToken"));
	}
}
