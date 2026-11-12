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
Uma conta é um lugar onde o dinheiro está: carteira, conta corrente, poupança, cartão de crédito ou outro. Toda transação pertence a uma conta (`accountId`).

| Rota | O que faz |
|---|---|
| `GET /api/accounts` | Lista as contas do usuário, cada uma com o `balance` atual |
| `GET /api/accounts/{id}` | Detalhe de uma conta |
| `POST /api/accounts` | Cria uma conta |
| `PUT /api/accounts/{id}` | Edita uma conta |
| `DELETE /api/accounts/{id}` | Exclui. Se a conta tem transações ou pagamentos de fatura: 409 |

**Corpo (criar e editar):** `name`, `type` e, opcionalmente, `initialBalance` (o saldo com que a conta começa). O tipo (`type`) é um destes:
- `WALLET`: dinheiro em mãos.
- `CHECKING`: conta corrente.
- `SAVINGS`: poupança.
- `CREDIT_CARD`: cartão de crédito. Só ele aceita (e exige) `creditLimit`, `closingDay` e `dueDay`; ver "Cartão e fatura" abaixo.
- `OTHER`: qualquer outra.

**Saldo (`balance`).** Você nunca envia o saldo: ele é calculado a cada consulta.
`balance` = saldo inicial + receitas − despesas − pagamentos de fatura que saíram da conta + pagamentos de fatura que entraram nela.
Num cartão de crédito, as compras são despesas e os pagamentos de fatura recebidos entram a favor.

**Exemplo.** Uma conta corrente começa com 100,00 (`initialBalance`). Entra uma receita de 50,00 e sai uma despesa de 30,00: o `balance` é 120,00. Se dessa conta você pagar 200,00 da fatura de um cartão, o `balance` da conta cai 200,00 e o do cartão sobe 200,00. Esse pagamento não conta como despesa nem como receita nos relatórios, para o gasto não aparecer em dobro.

## Cartão e fatura
Um cartão de crédito é uma conta do tipo `CREDIT_CARD` (ver "Contas"). A **fatura** não é uma conta: é a visão mensal do cartão, calculada a partir das compras. O mês da fatura (`AAAA-MM`) é o do **fechamento**.

| Rota | O que faz |
|---|---|
| `GET /api/cards` | Lista os cartões, cada um com limite, limite usado, limite disponível, saldo, fatura atual (aberta) e fatura anterior |
| `GET /api/accounts/{id}/invoices/{AAAA-MM}` | Mostra uma fatura: período, fechamento, vencimento, status, total, pago, restante, compras e pagamentos |
| `POST /api/accounts/{id}/invoices/{AAAA-MM}/payments` | Registra o pagamento da fatura |
| `DELETE /api/accounts/{id}/invoices/{AAAA-MM}/payments/{paymentId}` | Desfaz um pagamento |

**Campos do cartão.** Ao criar uma conta `CREDIT_CARD`: `creditLimit` (limite), `closingDay` (dia de fechamento) e `dueDay` (dia de vencimento), todos obrigatórios, com os dias de 1 a 28. Nos outros tipos de conta esses campos são proibidos.

**Status da fatura (`status`).** `OPEN` (ainda não fechou), `CLOSED` (passou o fechamento e falta pagar) e `PAID` (paga).

**Pagamento.** Corpo: `sourceAccountId` (conta de onde sai o dinheiro, que não pode ser um cartão), `amount` e `paidOn` (opcional).
- Só vale para fatura fechada.
- O valor não pode passar do que resta.
- Compra em fatura que já tem pagamento não pode ser criada, editada nem excluída (409).

**Exemplo.** Cartão com fechamento no dia 25 e vencimento no dia 10. Uma compra de 10/09 entra na fatura `2026-09`, que fecha em 25/09. Uma compra de 26/09 já entra na fatura `2026-10`. Depois de 25/09 a fatura `2026-09` fica `CLOSED`, e então é possível pagá-la. Pagando uma parte, ela segue `CLOSED` com o `restante` menor. Quando a soma dos pagamentos cobre o total, vira `PAID`.

## Categorias (`/api/categories`)
Uma categoria classifica uma transação (por exemplo, Alimentação). Ao cadastrar o usuário, o sistema já cria um conjunto padrão.

| Rota | O que faz |
|---|---|
| `GET /api/categories` | Lista |
| `GET /api/categories/{id}` | Detalhe |
| `POST /api/categories` | Cria |
| `PUT /api/categories/{id}` | Edita (só o `name` muda) |
| `DELETE /api/categories/{id}` | Exclui. Se está em uso: 409 |

**Corpo:** `name`, `kind` (`INCOME` para receita ou `EXPENSE` para despesa) e `parentId` (opcional, para criar uma subcategoria). Só existe **um nível** de subcategoria. O nome é único por usuário, sem diferenciar maiúsculas de minúsculas.

## Formas de pagamento (`/api/payment-methods`)
Como a pessoa pagou: Pix, débito, crédito, dinheiro. O sistema cria as padrão no cadastro. As rotas são as mesmas das categorias (listar, consultar, criar, editar, excluir), em `/api/payment-methods`. Os campos aparecem no Swagger. O nome é único por usuário, sem diferenciar maiúsculas de minúsculas, e excluir uma forma de pagamento em uso dá 409.

## Transações (`/api/transactions`)
Uma transação é uma receita ou despesa: valor, data, categoria e conta.

| Rota | O que faz |
|---|---|
| `GET /api/transactions` | Lista com filtros e paginação (abaixo) |
| `GET /api/transactions/{id}` | Detalhe |
| `POST /api/transactions` | Cria |
| `PUT /api/transactions/{id}` | Edita |
| `DELETE /api/transactions/{id}` | Exclui |

**Corpo (criar e editar):** `accountId`, `categoryId`, `paymentMethodId` (opcional), `type`, `amount`, `occurredOn`, `merchant` (estabelecimento) e `description`.

**Regras**
- `amount` é maior que zero, com no máximo 2 casas decimais.
- O `type` deve ser o mesmo da categoria escolhida; senão, 400.
- `occurredOn` entre 01/01/2000 e hoje mais 1 ano.
- A conta, a categoria e a forma de pagamento têm de ser do mesmo usuário.

**Listagem.** Todos os filtros são opcionais:
- `from` e `to`: período.
- `categoryId`: se for uma categoria pai, inclui as subcategorias.
- `type`, `accountId` e `q` (texto, procurado no estabelecimento e na descrição).
- `page` (começa em 0) e `size` (padrão 20, máximo 100).

A ordem é da data mais nova para a mais antiga. A resposta traz `content`, `page`, `size`, `totalElements` e `totalPages`.

**Exemplo de corpo (criar):**
```json
{
  "accountId": "<id da conta>",
  "categoryId": "<id da categoria>",
  "type": "EXPENSE",
  "amount": 42.90,
  "occurredOn": "2026-11-11",
  "merchant": "Mercado",
  "description": "Compras da semana"
}
```

## Orçamentos (`/api/budgets`)
Um orçamento é um limite de gasto para um mês, geral ou para uma categoria. O gasto atual é calculado no backend, a partir das despesas do mês.

| Rota | O que faz |
|---|---|
| `GET /api/budgets?month=AAAA-MM` | Lista os orçamentos do mês (sem `month`, usa o mês de hoje) |
| `GET /api/budgets/{id}` | Detalhe |
| `POST /api/budgets` | Cria |
| `PUT /api/budgets/{id}` | Edita (só o `limit` muda) |
| `DELETE /api/budgets/{id}` | Exclui |

**Corpo (criar):** `month` (`AAAA-MM`), `limit` (o valor máximo) e `categoryId` (opcional; sem ele, o orçamento é **geral**). Só categorias de despesa. Só pode existir um orçamento por usuário, mês e categoria; repetir dá 409.

**Cada item devolve:** `spent` (gasto no mês), `remaining` (quanto resta), `percentUsed` (percentual usado) e `status`:
- `OK`: abaixo de 80% do limite.
- `WARNING`: de 80% do limite em diante.
- `EXCEEDED`: acima do limite.

**Exemplo.** Orçamento de 500,00 em Alimentação para novembro. Com 420,00 gastos, `remaining` é 80,00, `percentUsed` é 84% e o `status` é `WARNING`. Passando de 500,00, vira `EXCEEDED`.
