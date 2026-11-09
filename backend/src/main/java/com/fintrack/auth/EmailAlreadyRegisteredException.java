package com.fintrack.auth;

class EmailAlreadyRegisteredException extends RuntimeException {

	EmailAlreadyRegisteredException() {
		super("E-mail já cadastrado");
	}
}
