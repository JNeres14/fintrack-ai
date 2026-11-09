package com.fintrack.auth;

import com.fintrack.auth.AuthDtos.LoginRequest;
import com.fintrack.auth.AuthDtos.RefreshRequest;
import com.fintrack.auth.AuthDtos.RegisterRequest;
import com.fintrack.auth.AuthDtos.RegisterResponse;
import com.fintrack.auth.AuthDtos.TokenResponse;
import com.fintrack.user.UserService.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AuthService service;
	private final RateLimiter limiter;

	AuthController(AuthService service, RateLimiter limiter) {
		this.service = service;
		this.limiter = limiter;
	}

	/**
	 * O IP vem de getRemoteAddr(): cabeçalhos como X-Forwarded-For são ignorados de propósito, porque o
	 * cliente poderia forjá-los para escapar do limite. Atrás de proxy (Fase 6) isso será revisto.
	 * O limite é checado antes de qualquer acesso ao banco ou ao BCrypt.
	 */
	private void limit(String endpoint, HttpServletRequest request) {
		limiter.check(endpoint + ":" + request.getRemoteAddr());
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	RegisterResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
		limit("register", request);
		UserView user = service.register(body.email(), body.password());
		return new RegisterResponse(user.id().toString(), user.email());
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
		limit("login", request);
		return service.login(body.email(), body.password());
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest body, HttpServletRequest request) {
		limit("refresh", request);
		return service.refresh(body.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody RefreshRequest body, HttpServletRequest request) {
		limit("logout", request);
		service.logout(body.refreshToken());
	}
}
