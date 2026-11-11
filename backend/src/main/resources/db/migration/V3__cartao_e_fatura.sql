-- V3: cartão de crédito e pagamento de fatura (ADR-016).
-- A fatura não é gravada: é calculada pelas transações no período. Só o pagamento é registro.

ALTER TABLE accounts
    ADD COLUMN credit_limit NUMERIC(14,2),
    ADD COLUMN closing_day  INTEGER,
    ADD COLUMN due_day      INTEGER;

-- O CHECK abaixo exige limite e dias para todo cartão. Um cartão criado antes desta migração não tem esses dados
-- e não pode ser inventado: vira conta do tipo OTHER (nenhuma transação é alterada).
UPDATE accounts SET type = 'OTHER' WHERE type = 'CREDIT_CARD';

-- Cartão: os três campos obrigatórios (dias de 1 a 28). Outros tipos: os três proibidos.
-- IS NOT NULL explícito em cada campo: um CHECK que dá NULL (e não false) passaria sem barrar nada.
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_card_fields CHECK (
    (type = 'CREDIT_CARD'
        AND credit_limit IS NOT NULL AND credit_limit >= 0
        AND closing_day IS NOT NULL AND closing_day BETWEEN 1 AND 28
        AND due_day IS NOT NULL AND due_day BETWEEN 1 AND 28)
    OR
    (type <> 'CREDIT_CARD'
        AND credit_limit IS NULL AND closing_day IS NULL AND due_day IS NULL)
);

-- invoice_month guarda sempre o dia 1 do mês de FECHAMENTO da fatura.
-- FKs compostas por usuário, como no resto do schema: o banco recusa conta de outro usuário.
CREATE TABLE invoice_payments (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL REFERENCES users (id),
    card_account_id   UUID          NOT NULL,
    source_account_id UUID          NOT NULL,
    invoice_month     DATE          NOT NULL,
    amount            NUMERIC(14,2) NOT NULL,
    paid_on           DATE          NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoice_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_invoice_payments_month_first_day CHECK (EXTRACT(DAY FROM invoice_month) = 1),
    CONSTRAINT ck_invoice_payments_distinct_accounts CHECK (card_account_id <> source_account_id),
    CONSTRAINT fk_invoice_payments_card FOREIGN KEY (card_account_id, user_id) REFERENCES accounts (id, user_id),
    CONSTRAINT fk_invoice_payments_source FOREIGN KEY (source_account_id, user_id) REFERENCES accounts (id, user_id)
);
CREATE INDEX ix_invoice_payments_card_month ON invoice_payments (user_id, card_account_id, invoice_month);
CREATE INDEX ix_invoice_payments_source ON invoice_payments (user_id, source_account_id);
