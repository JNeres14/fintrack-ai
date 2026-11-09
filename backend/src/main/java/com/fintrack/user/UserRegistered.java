package com.fintrack.user;

import java.util.UUID;

/** Publicado ao criar um usuário. Os outros módulos ouvem para criar os dados padrão (categorias, contas...). */
public record UserRegistered(UUID userId) {
}
