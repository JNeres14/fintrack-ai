# API do FinTrack AI (Fase 2)

Referência dos endpoints. A versão navegável (Swagger UI) existe **só no perfil `dev`**: `http://localhost:8080/swagger-ui.html` (JSON em `/v3/api-docs`). Decisões: ADRs 010 a 016, 018 e 019 em `docs/architecture/adr/`.

## Convenções
- JSON; erros em `application/problem+json` (`title`, `detail`, `errors` nos de validação). Nunca trazem stack trace nem valor enviado.
- **Autenticação:** `Authorization: Bearer <accessToken>` em tudo, exceto `/api/auth/*` (POST) e `GET /actuator/health`. O dono dos dados é sempre o usuário do token; o corpo nunca leva `userId` (se enviado, é ignorado).
- **Isolamento:** recurso de outro usuário responde **404**, nunca 403.
- **Dinheiro:** número com no máximo 2 casas decimais (mais que isso = 400; nada é arredondado em silêncio). Datas `AAAA-MM-DD`; mês `AAAA-MM`.
- **Códigos:** 400 validação ou regra de negócio, 401 sem token ou token inválido, 404 inexistente ou de outro usuário, 409 conflito (nome repetido, item em uso, fatura com pagamento), 429 limite de requisições (com `Retry-After`).

## Autenticação (`/api/auth`)
| Método e rota | O que faz |
|---|---|
| `POST /register` | Cadastra (`email`, `password`). 201; 409 se o e-mail já existe; senha de 12 a 72 bytes. Cria categorias, formas de pagamento e a conta "Carteira" padrão. |
| `POST /login` | Devolve `accessToken` (15 min), `refreshToken` (7 dias) e `expiresInSeconds`. 5 falhas seguidas bloqueiam o e-mail por 15 min (a resposta segue 401 genérica). |
| `POST /refresh` | Troca o refresh por um par novo (rotação). Refresh já usado revoga a sessão inteira (401). |
| `POST /logout` | Revoga a sessão do refresh enviado. Sempre 204. |

Rate limit: 10 por minuto por IP e por endpoint (429).

## Contas (`/api/accounts`)
`GET` (lista), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`. Corpo: `name`, `type` (`WALLET`, `CHECKING`, `SAVINGS`, `CREDIT_CARD`, `OTHER`), `initialBalance` (opcional). O `balance` é calculado: saldo inicial + receitas − despesas − pagamentos de fatura feitos + pagamentos recebidos. Excluir conta com transações ou pagamentos: 409.

## Cartão e fatura
- Conta `CREDIT_CARD` exige `creditLimit`, `closingDay` e `dueDay` (1 a 28); nos outros tipos esses campos são proibidos.
- `GET /api/cards`: limite, usado, disponível, saldo, fatura atual (aberta) e anterior.
- `GET /api/accounts/{id}/invoices/{AAAA-MM}`: período, fechamento, vencimento, status (`OPEN`, `CLOSED`, `PAID`), total, pago, restante, compras e pagamentos. O mês é o do **fechamento**.
- `POST /api/accounts/{id}/invoices/{AAAA-MM}/payments`: `sourceAccountId`, `amount`, `paidOn` (opcional). Só fatura fechada, valor até o restante, origem que não seja cartão.
- `DELETE /api/accounts/{id}/invoices/{AAAA-MM}/payments/{paymentId}`.
- Compra em fatura que já tem pagamento não pode ser criada, editada nem excluída (409).

## Categorias (`/api/categories`) e formas de pagamento (`/api/payment-methods`)
CRUD completo. Categoria: `name`, `kind` (`INCOME`/`EXPENSE`), `parentId` opcional (um nível); na edição só o nome muda. Nomes únicos por usuário (sem diferenciar maiúsculas). Excluir item em uso: 409.

## Transações (`/api/transactions`)
CRUD. Corpo: `accountId`, `categoryId`, `paymentMethodId` (opcional), `type`, `amount` (> 0), `occurredOn`, `merchant`, `description`. O `type` deve ser o da categoria; data entre 01/01/2000 e hoje + 1 ano.

Listagem `GET`: filtros opcionais `from`, `to`, `categoryId` (o pai inclui as subcategorias), `type`, `accountId`, `q` (texto) e paginação `page` (a partir de 0) e `size` (padrão 20, máximo 100). Ordem: data decrescente. Resposta: `content`, `page`, `size`, `totalElements`, `totalPages`.

## Orçamentos (`/api/budgets`)
`GET ?month=AAAA-MM` (padrão: mês de hoje), `GET /{id}`, `POST` (`month`, `categoryId` opcional = geral, `limit`), `PUT /{id}` (só `limit`), `DELETE /{id}`. Cada item traz `spent`, `remaining`, `percentUsed` e `status` (`OK`, `WARNING` a partir de 80%, `EXCEEDED` acima do limite), calculados no backend a partir das despesas do mês. Só categorias de despesa; um orçamento por usuário, mês e categoria (409).
