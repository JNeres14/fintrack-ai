package com.fintrack.auth;

import java.util.List;

import com.fintrack.shared.GlobalExceptionHandler.FieldProblem;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Precedência alta: sem ela, o handler genérico (Exception.class) do módulo shared pegaria estes erros antes. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler {

	@ExceptionHandler(AuthenticationFailedException.class)
	ResponseEntity<ProblemDetail> handleAuthFailed(AuthenticationFailedException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
		problem.setTitle("Não autenticado");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.body(problem);
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ProblemDetail handleEmailExists(EmailAlreadyRegisteredException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Já existe um cadastro com este e-mail. Esqueceu a senha?");
		problem.setTitle("E-mail já cadastrado");
		return problem;
	}

	@ExceptionHandler(InvalidPasswordException.class)
	ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
		problem.setTitle("Validação falhou");
		problem.setProperty("errors", List.of(new FieldProblem("password", ex.getMessage())));
		return problem;
	}

	@ExceptionHandler(TooManyRequestsException.class)
	ResponseEntity<ProblemDetail> handleTooMany(TooManyRequestsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
				"Muitas tentativas. Aguarde um pouco e tente de novo.");
		problem.setTitle("Muitas requisições");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds())).body(problem);
	}
}
