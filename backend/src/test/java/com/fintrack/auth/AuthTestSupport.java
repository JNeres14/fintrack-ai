package com.fintrack.auth;


import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Chamadas HTTP repetidas dos testes de autenticação. */
final class AuthTestSupport {

	static final String PASSWORD = "SenhaDeTeste-2026!";

	private AuthTestSupport() {
	}

	private static String json(String... kv) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < kv.length; i += 2) {
			sb.append(i > 0 ? "," : "").append('"').append(kv[i]).append("\":\"").append(kv[i + 1]).append('"');
		}
		return sb.append('}').toString();
	}

	static ResultActions post(MockMvc mvc, String path, String... kv) throws Exception {
		return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json(kv)));
	}

	static ResultActions register(MockMvc mvc, String email, String password) throws Exception {
		return post(mvc, "/api/auth/register", "email", email, "password", password);
	}

	static ResultActions login(MockMvc mvc, String email, String password) throws Exception {
		return post(mvc, "/api/auth/login", "email", email, "password", password);
	}

	static ResultActions refresh(MockMvc mvc, String refreshToken) throws Exception {
		return post(mvc, "/api/auth/refresh", "refreshToken", refreshToken);
	}

	static String field(ResultActions result, String jsonPath) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
	}
}
