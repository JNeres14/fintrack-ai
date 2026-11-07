## 6. Modelo de dados

O PostgreSQL guarda 10 entidades, com `Transaction` no centro. Todos os atributos são um ponto de partida para o Flyway, não um esquema final. Todas as tabelas de dados levam `user_id`: o isolamento por usuário vale sempre.

```mermaid
erDiagram
    User ||--o{ Account : possui
    User ||--o{ Goal : define
    Account ||--o{ Transaction : contem
    Category ||--o{ Transaction : classifica
    Category ||--o{ Budget : limita
    PaymentMethod ||--o{ Transaction : usa
    RecurringTransaction ||--o{ Transaction : gera
    Transaction |o--o| Attachment : comprova
    Attachment ||--o{ AIAnalysis : gera

    User {
        uuid id PK
        string email
        string password_hash
        timestamptz created_at
    }
    Account {
        uuid id PK
        uuid user_id FK
        string name
        string type
        numeric initial_balance
    }
    Transaction {
        uuid id PK
        uuid user_id FK
        uuid account_id FK
        uuid category_id FK
        uuid payment_method_id FK
        string type
        numeric amount "NUMERIC(14,2)"
        date occurred_on
        string merchant
        string description
        uuid attachment_id FK
    }
    Category {
        uuid id PK
        uuid user_id FK
        string name
        string kind "receita ou despesa"
        uuid parent_id FK "subcategoria"
    }
    PaymentMethod {
        uuid id PK
        uuid user_id FK
        string name "Pix, cartao..."
    }
    Attachment {
        uuid id PK
        uuid user_id FK
        string storage_key
        string sha256
        string mime_type
        int size
        timestamptz uploaded_at
    }
    AIAnalysis {
        uuid id PK
        uuid attachment_id FK
        string status
        string model
        numeric confidence
        jsonb result_json
        timestamptz created_at
    }
    Budget {
        uuid id PK
        uuid user_id FK
        uuid category_id FK "opcional"
        numeric limit_amount
        date month
    }
    Goal {
        uuid id PK
        uuid user_id FK
        string name
        numeric target_amount
        date deadline
        string status
    }
    RecurringTransaction {
        uuid id PK
        uuid user_id FK
        uuid account_id FK
        uuid category_id FK
        string type
        numeric amount
        string frequency
        date next_run_date
        boolean active
    }
```

Um usuário tem várias contas, e cada conta tem várias transações. Uma transação tem uma categoria, uma forma de pagamento e, opcionalmente, um comprovante; cada comprovante pode gerar uma ou mais análises de IA (mais de uma permite reprocessar a imagem).

**Decisões de modelagem**

- Valores monetários em `NUMERIC(14,2)` no banco e `BigDecimal` no Java; nunca `FLOAT` ou `DOUBLE`.
- Chaves primárias em UUID, sem IDs sequenciais expostos.
- `occurred_on` é uma `DATE` (o dia do gasto, sem fuso); `created_at` e `uploaded_at` são `TIMESTAMPTZ` em UTC.
- `user_id` em todas as tabelas de dados, com índices como `(user_id, occurred_on)` e `(user_id, category_id)`.
- O saldo da conta é calculado (saldo inicial + receitas − despesas), não armazenado, para não divergir do histórico.
- `amount` é sempre positivo (`CHECK amount > 0`); o sinal vem de `type` (receita ou despesa).
- `Attachment` tem unicidade em `(user_id, sha256)`, que sustenta a detecção de imagem duplicada.
- `AIAnalysis` guarda o JSON validado, o status (pendente, revisada, confirmada, descartada), o modelo e os tokens usados; a imagem fica só no armazenamento.
- Subcategorias usam `parent_id` na própria tabela `Category`.
- Migrações versionadas em `V1__...sql`, `V2__...sql`, e nenhuma alteração manual no banco.

