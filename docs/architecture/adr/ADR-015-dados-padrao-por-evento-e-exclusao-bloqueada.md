# ADR-015: Dados padrão por evento, exclusão bloqueada e saldo calculado

Status: proposta (Fase 2, Bloco C, 09/11/2026), aprovada no chat; aguardando commit

## Decisão
- **Dados padrão:** `UserService.create` publica `UserRegistered`; os módulos `category` e `transaction` ouvem (síncrono, mesma transação do cadastro) e criam as categorias (9 de despesa com 6 subcategorias, 3 de receita), as formas de pagamento (Pix, Débito, Crédito, Dinheiro) e uma conta "Carteira" (toda transação exige conta). Se algo falhar, o cadastro desfaz. O módulo `auth` não conhece esses módulos.
- **Saldo da conta:** calculado a cada consulta no banco (saldo inicial + receitas - despesas), nunca armazenado. A listagem usa uma consulta agrupada.
- **Exclusão de item em uso:** bloqueada com 409 (conta e forma de pagamento com transações; categoria com transações, subcategorias ou orçamentos). Sem exclusão em cascata nem arquivamento no MVP.
- **Categorias:** um nível de subcategoria; o pai deve ser raiz, do mesmo tipo e do mesmo usuário; só o nome é editável (tipo e pai são fixos).
- **Nomes:** únicos por usuário sem diferenciar maiúsculas (por escopo, no caso das categorias); o serviço checa antes e o índice único do banco decide em corrida.
- **Isolamento:** recurso de outro usuário responde 404. Categoria usa transações e orçamentos só pelos services públicos (`TransactionService`, `BudgetService`).

## Consequências
Usuários criados direto no banco ou pelo repository (fora da API) não recebem os padrões. Sem arquivamento, quem quiser "aposentar" uma categoria com histórico não consegue; reavaliar se virar necessidade.
