# ADR-016: Cartão de crédito e fatura

Status: aceita (aprovada pelo Neres em 11/11/2026; implementada na Fase 2)
Base: `specs/SPEC-cartao-e-cashback.md` (seção 2). Onde este ADR for mais específico, ele detalha a spec; onde divergir, vale a spec.

## Contexto
Boa parte dos gastos do usuário é no cartão de crédito. Hoje o cartão é só uma conta do tipo `CREDIT_CARD` com saldo negativo. Falta agrupar as compras por fatura, mostrar limite e registrar o pagamento sem contar o gasto em dobro. Sem juros, multa, rotativo, parcelas (D3) nem cashback (Fase 3).

## Decisão

### 1. Dados do cartão
- A conta `CREDIT_CARD` ganha `credit_limit` (`NUMERIC(14,2)`), `closing_day` e `due_day` (1 a 28). Os três são obrigatórios para cartão e **proibidos** nos outros tipos (`CHECK` no banco e validação na API). Cada cartão é uma conta, então vários cartões já cabem.
- Migração V3 (sem editar V1 nem V2). O saldo inicial do cartão é a dívida já existente (negativo).
- **Migração e dados já existentes:** como o `CHECK` exige os três campos para cartão, a V3 converte qualquer conta `CREDIT_CARD` já existente em um banco de desenvolvimento para `OTHER` (não invento limite nem dias). O banco de teste é refeito do zero. **Pergunta ao Neres:** existe conta de cartão no seu banco `fintrack`? Se existir, avise antes de aplicar.
- Alterações no cartão: mudar `closing_day` é **bloqueado (409)** se o cartão já tem algum pagamento de fatura (reclassificaria compras entre faturas, inclusive pagas). Mudar `due_day` e o limite é sempre permitido. Mudar o tipo de `CREDIT_CARD` para outro, ou o contrário, é bloqueado se a conta já tem transações ou pagamentos.

### 2. Nome da fatura (`AAAA-MM`) e datas
- **`AAAA-MM` é o mês do FECHAMENTO.** A fatura `2026-12` é a que fecha em dezembro de 2026. Motivo: a fatura de uma compra depende só da data da compra e do dia de fechamento, sem depender do vencimento. Usar o mês do vencimento mudaria o nome da fatura quando o dia de vencimento mudar.
- **Período:** do dia seguinte ao fechamento do mês anterior até o dia de fechamento do mês, inclusive. Compra no dia de fechamento ou antes entra na fatura do mês; depois, na do mês seguinte.
- **Vencimento:** no mesmo mês do fechamento se `due_day > closing_day`; no **mês seguinte** se `due_day <= closing_day` (inclusive quando são iguais).
- Exemplos (testados): fecha 25, vence 5: a fatura `2026-12` vai de 26/11 a 25/12 e vence em 05/01/2027. Fecha 5, vence 10: `2026-12` vai de 06/11 a 05/12 e vence em 10/12. Fecha 10, vence 10: vence no mês seguinte. Compra em 26/12/2026 com fechamento no dia 25 cai em `2027-01` (virada de ano).
- "Hoje" usa o fuso `America/Sao_Paulo` (configurável por `APP_TIMEZONE`). O `Clock` UTC atual segue valendo para o resto.

### 3. Status e valores da fatura
- **Total** = despesas menos receitas (estornos) da conta no período. Pode ser zero ou negativo (crédito).
- **Aberta:** hoje <= data de fechamento (inclui faturas futuras, vindas de compras com data futura).
- **Fechada:** hoje > fechamento e pagamentos < total.
- **Paga:** hoje > fechamento e pagamentos >= total (inclui fatura fechada com total zero ou negativo). Uma fatura aberta nunca é "paga".
- **Limite usado** = dívida total do cartão (saldo devedor), incluindo tudo que ainda não foi pago e compras com data futura; é a mesma conta do saldo, sem rodar de novo por fatura. **Disponível** = limite menos usado (pode ficar negativo se estourar o limite; a API mostra o número real).

### 4. Pagamento de fatura (`invoice_payments`, registro próprio)
- Campos: cartão, **conta de origem**, **fatura** (mês do fechamento), valor (`NUMERIC(14,2)`, maior que zero) e **data do pagamento**. `FK` compostas com `user_id`, como no resto do schema, e `CHECK` cartão diferente da origem.
- A origem não pode ser um cartão (validado na API; pagar cartão com cartão não existe no MVP).
- **Não é receita nem despesa:** fica fora dos relatórios de gasto e de renda.
- **Saldo calculado das contas** passa a ser: saldo inicial + receitas − despesas − pagamentos feitos com a conta como origem + pagamentos recebidos pelo cartão. Ou seja, o pagamento reduz a conta de origem e reduz a dívida do cartão (saldo do cartão sobe em direção a zero). O cálculo continua no SQL, nunca armazenado.
- **Pagamento de fatura ainda aberta: bloqueado (400).** O total só é definitivo no fechamento, e uma fatura "paga" que ainda recebe compras ficaria instável. Pagar antecipado para liberar limite fica fora do MVP.
- **Pagamento maior que o saldo da fatura (total − já pago): bloqueado (400).** Sem crédito ou sobra no MVP.
- **Pagamento de fatura com total zero ou negativo: bloqueado (400)** (nada a pagar).
- **Pagamento parcial:** permitido; vários pagamentos somam. A fatura vira "paga" quando a soma cobre o total.
- **Data do pagamento:** entre a data de fechamento da fatura e hoje, inclusive (padrão: hoje).
- **Exclusão de um pagamento (proposta fora da lista da spec):** `DELETE .../payments/{paymentId}`. Sem ele, um pagamento lançado errado trancaria a fatura para sempre. **Preciso do seu ok para incluir.**
- Excluir uma conta com pagamentos (como cartão ou como origem) é bloqueado (409), como já é com transações.

### 5. Compra em fatura já paga
- Criar, editar ou excluir uma compra (transação na conta do cartão) é **bloqueado (409, mensagem clara)** quando a fatura afetada **já tem qualquer pagamento**, parcial ou total. Regra mais estrita que "paga", de propósito: com pagamento parcial, mudar o total também deixaria a fatura inconsistente. Fatura fechada **sem** pagamento pode ser corrigida livremente.
- Na edição valem as duas faturas: a de origem e a de destino (se mudou data ou cartão). As duas precisam estar sem pagamento.
- Vale também para as parcelas do D3 quando existirem.
- Para evitar corrida entre pagamento e edição, as duas operações travam a linha da conta do cartão (`PESSIMISTIC_WRITE`) durante a transação.

### 6. Endpoints (todos exigem token; outro usuário recebe 404)
- `POST` e `PUT /api/accounts`: ganham `creditLimit`, `closingDay` e `dueDay`.
- `GET /api/cards`: por cartão, limite, usado, disponível, saldo, dias e a fatura atual (mês, status, total, vencimento, falta pagar). Alimenta a aba da Fase 3.
- `GET /api/accounts/{id}/invoices/{AAAA-MM}`: período, fechamento, vencimento, status, total, pago, falta pagar, as compras e os pagamentos. Conta que não é cartão (ou de outro usuário) responde 404; mês mal formado, 400.
- `POST /api/accounts/{id}/invoices/{AAAA-MM}/payments`: corpo `sourceAccountId`, `amount` e `paidOn` (opcional). 201.
- Sem paginação na fatura: o tamanho é limitado pelo período de um mês.

## Consequências
- O saldo das contas muda de fórmula (inclui pagamentos). Os testes de saldo do Bloco C são atualizados junto.
- `TransactionService` passa a consultar as faturas antes de criar, editar e excluir (um único ponto, `InvoiceGuard`).
- Faturas não são gravadas: são calculadas a cada consulta (só o pagamento é registro). Simples e sem dado duplicado.
- Limitações aceitas: sem juros, multa, rotativo, crédito de pagamento a maior, pagamento antecipado, parcelas (D3) e cashback (Fase 3).

## Testes previstos
Fechamento no dia exato e no dia seguinte; virada de ano; vencimento no mesmo mês, no mês seguinte e com dias iguais; fatura aberta, fechada e paga; pagamento parcial e total; pagamento maior que o saldo, de fatura aberta e de fatura zerada (400); saldo da origem e do cartão após pagar (inclusive centavos); bloqueio de compra em fatura com pagamento (criar, editar, excluir e mover entre faturas); mudança de `closing_day` bloqueada; limite usado e disponível; campos de cartão proibidos em conta comum; acesso cruzado em todo endpoint novo (inclusive usar conta de origem de outro usuário); 401 sem token.

## Alternativas descartadas
- Nome da fatura pelo mês do vencimento (depende de dois dias e muda se o vencimento mudar).
- Gravar a fatura como registro (duplica dado calculável e pede reconciliação).
- Pagamento como despesa ou receita (conta o gasto em dobro e infla a renda).
- Permitir pagamento de fatura aberta ou maior que o saldo (estado instável e crédito sem modelo).
