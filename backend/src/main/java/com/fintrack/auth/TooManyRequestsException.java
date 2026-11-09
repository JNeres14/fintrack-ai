package com.fintrack.auth;

class TooManyRequestsException extends RuntimeException {

	private final long retryAfterSeconds;

	TooManyRequestsException(long retryAfterSeconds) {
		super("Muitas requisições");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
