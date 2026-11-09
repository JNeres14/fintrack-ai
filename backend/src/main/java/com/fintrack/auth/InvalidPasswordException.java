package com.fintrack.auth;

class InvalidPasswordException extends RuntimeException {

	InvalidPasswordException(String message) {
		super(message);
	}
}
