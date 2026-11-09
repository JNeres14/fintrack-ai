-- V1: tabelas base da Fase 2 (docs/database/01-modelo-de-dados.md).
-- Sem attachment_id (entra com Attachment na Fase 4). Moeda única (BRL), sem coluna de moeda.
-- gen_random_uuid() é nativo desde o PostgreSQL 13; a aplicação gera os UUIDs, o default serve a inserts manuais.
--
-- Isolamento no banco: tabelas referenciadas têm UNIQUE (id, user_id) e as chaves estrangeiras das tabelas
-- filhas são compostas (x_id, user_id). Assim o banco recusa, por exemplo, uma transação do usuário A
-- apontando para uma conta do usuário B.

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE accounts (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id),
    name            VARCHAR(100)  NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    initial_balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_accounts_type CHECK (type IN ('WALLET', 'CHECKING', 'SAVINGS', 'CREDIT_CARD', 'OTHER')),
    CONSTRAINT uq_accounts_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX ux_accounts_user_name ON accounts (user_id, lower(name));

CREATE TABLE categories (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id),
    name       VARCHAR(100) NOT NULL,
    kind       VARCHAR(10)  NOT NULL,
    parent_id  UUID,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_categories_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_categories_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT uq_categories_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id, user_id) REFERENCES categories (id, user_id)
);
-- Nome único por usuário, tipo e nível (PostgreSQL trata NULL como distinto, daí o COALESCE).
CREATE UNIQUE INDEX ux_categories_user_kind_parent_name
    ON categories (user_id, kind, COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name));

CREATE TABLE payment_methods (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_methods_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX ux_payment_methods_user_name ON payment_methods (user_id, lower(name));

CREATE TABLE transactions (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL REFERENCES users (id),
    account_id        UUID          NOT NULL,
    category_id       UUID          NOT NULL,
    payment_method_id UUID,
    type              VARCHAR(10)   NOT NULL,
    amount            NUMERIC(14,2) NOT NULL,
    occurred_on       DATE          NOT NULL,
    merchant          VARCHAR(200),
    description       VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id, user_id) REFERENCES accounts (id, user_id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id, user_id) REFERENCES categories (id, user_id),
    CONSTRAINT fk_transactions_payment_method FOREIGN KEY (payment_method_id, user_id) REFERENCES payment_methods (id, user_id)
);
CREATE INDEX ix_transactions_user_occurred_on ON transactions (user_id, occurred_on);
CREATE INDEX ix_transactions_user_category ON transactions (user_id, category_id);
CREATE INDEX ix_transactions_user_account ON transactions (user_id, account_id);

-- category_id nulo = orçamento mensal geral. month guarda sempre o dia 1 do mês.
CREATE TABLE budgets (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          NOT NULL REFERENCES users (id),
    category_id  UUID,
    limit_amount NUMERIC(14,2) NOT NULL,
    month        DATE          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_budgets_limit CHECK (limit_amount > 0),
    CONSTRAINT ck_budgets_month_first_day CHECK (EXTRACT(DAY FROM month) = 1),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id, user_id) REFERENCES categories (id, user_id)
);
CREATE UNIQUE INDEX ux_budgets_user_month_general ON budgets (user_id, month) WHERE category_id IS NULL;
CREATE UNIQUE INDEX ux_budgets_user_month_category ON budgets (user_id, category_id, month) WHERE category_id IS NOT NULL;
