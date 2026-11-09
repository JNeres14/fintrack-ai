package com.fintrack.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs da API de autenticação. toString mascara senha e tokens para não vazarem em log por engano. */
final class AuthDtos {

	private AuthDtos() {
	}

	record RegisterRequest(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 128) String password) {

		@Override
		public String toString() {
			return "RegisterRequest[***]";
		}
	}

	record LoginRequest(
			@NotBlank @Size(max = 254) String email,
			@NotBlank @Size(max = 128) String password) {

		@Override
		public String toString() {
			return "LoginRequest[***]";
		}
	}

	record RefreshRequest(@NotBlank @Size(max = 256) String refreshToken) {

		@Override
		public String toString() {
			return "RefreshRequest[***]";
		}
	}

	record RegisterResponse(String id, String email) {
	}

	record TokenResponse(String tokenType, String accessToken, long expiresInSeconds, String refreshToken) {

		@Override
		public String toString() {
			return "TokenResponse[***]";
		}
	}
}
