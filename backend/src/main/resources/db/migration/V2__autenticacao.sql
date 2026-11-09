-- V2: autenticação (ADR-012 e ADR-013).
-- Bloqueio temporário por tentativas falhas: contador e instante de desbloqueio ficam no usuário.
-- Refresh token: o valor nunca é guardado, só o hash SHA-256 (64 caracteres hexadecimais).
-- family_id liga todos os tokens nascidos do mesmo login; reuso de um token já usado revoga a família.

ALTER TABLE users
    ADD COLUMN failed_attempts INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN locked_until    TIMESTAMPTZ;

CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id),
    family_id  UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
