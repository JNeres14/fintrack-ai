# ADR-018: Orçamentos mensais

Status: aprovada pelo Neres em 11/11/2026 e implementada no Bloco E (aguardando commit)

## Decisão
- `/api/budgets` (criar, buscar, listar por `?month=AAAA-MM`, editar só o limite, excluir). Geral (sem categoria) ou por categoria de despesa (receita = 400). Um por usuário, mês e categoria (409).
- Consumo no SQL: só despesas, pela data da compra; categoria pai inclui subcategorias; o geral soma tudo. Percentual com 1 casa (pode passar de 100). Status: OK, WARNING (>= 80%, comparação exata) e EXCEEDED (gasto > limite).
- Pai e filho podem ter orçamento ao mesmo tempo (sobrepõem-se).
- Arquitetura: budget usa transaction e category só por services públicos; cria ciclo de pacotes budget -> transaction -> category -> budget (não de beans), aceito como no Bloco D.
