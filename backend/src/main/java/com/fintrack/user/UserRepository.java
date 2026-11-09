package com.fintrack.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	/** O e-mail deve ser passado já normalizado (minúsculas). */
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
