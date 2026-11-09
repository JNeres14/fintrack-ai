package com.fintrack.support;

import com.fintrack.shared.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint protegido só de teste (existe apenas em src/test): devolve o userId que o CurrentUser enxerga. */
@RestController
class ProbeController {

	private final CurrentUser currentUser;

	ProbeController(CurrentUser currentUser) {
		this.currentUser = currentUser;
	}

	@GetMapping("/api/_probe/me")
	String me() {
		return currentUser.id().toString();
	}
}
