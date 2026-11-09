# SPEC — Cartão de crédito, fatura, parcelas e cashback

> Parte do `SPEC.md` (resumo geral). Detalha o que entra na Fase 2 e na Fase 3. Se este arquivo e o `specs/SPEC-fase-2.md` divergirem sobre cartão, vale este.
> Status: **proposta do Code aprovada por mim com ajustes (09/11/2026)**. Os ADRs 016 e 017 são escritos e aprovados antes do código do D2.

## 1. Onde cada parte entra
| Parte | Quando | Observação |
|---|---|---|
| Bloco D (transações) | Fase 2, próximo | Segue como está; o cartão depende dele |
| Bloco D2: cartão e fatura | Fase 2, depois do D e antes do E | Backend. A aba de cartão (tela) nasce na Fase 3 |
| Bloco E: orçamentos e fechamento | Fase 2 | Continua depois do D2 |
| Bloco D3: parcelas | Fase 2, **opcional**, só depois do E | Só entra se sobrar tempo; senão, início da Fase 3 |
| Cashback (D4) | **Início da Fase 3** | Backend antes da tela; ADR próprio nessa fase |

Fora do MVP: juros, multa, rotativo, antecipação ou quitação de parcelas.

## 2. Bloco D2: cartão e fatura
- Conta do tipo `CREDIT_CARD` ganha limite, dia de fechamento e dia de vencimento. Os três são obrigatórios para cartão e proibidos nos outros tipos, com `CHECK` no banco. Nova migração Flyway (sem editar a V1).
- Uma compra entra na fatura pela data: até o dia de fechamento (inclusive) vai na fatura do mês; depois dele, na do mês seguinte.
- Status da fatura: aberta, fechada (passou o fechamento, falta pagar) e paga.
- Pagamento de fatura é um registro próprio (`invoice_payments`), não receita nem despesa, para não contar o gasto em dobro. Reduz o saldo da conta de origem e o saldo devedor do cartão, e fica fora dos relatórios de gasto e receita.
- Pagamento parcial entra: a fatura vira "paga" quando a soma dos pagamentos cobre o total.
- Limite usado = tudo que ainda não foi pago, incluindo parcelas futuras.
- Endpoints previstos: `GET /api/cards`, `GET /api/accounts/{id}/invoices/{AAAA-MM}`, `POST /api/accounts/{id}/invoices/{AAAA-MM}/payments`.

### Decisões aprovadas (viram ADR 016)
| # | Decisão | Valor |
|---|---|---|
| 1 | Pagamento de fatura | Registro próprio (`invoice_payments`) |
| 2 | Dias de fechamento e vencimento | De 1 a 28 |
| 3 | Fuso horário para "hoje" | `America/Sao_Paulo`, configurável |
| 4 | Sobra de centavos nas parcelas | Nas primeiras parcelas |
| 5 | Vários cartões | Sim, cada cartão é uma conta |
| 6 | Antecipar ou quitar parcelas | Fora desta entrega |

### Ajustes que o ADR 016 precisa deixar explícitos
- **Nome da fatura (`AAAA-MM`):** definir se é o mês do fechamento ou do vencimento. Definir também que o vencimento cai no mês seguinte quando o dia de vencimento é menor ou igual ao de fechamento. Com teste.
- **Pagamento:** precisa de conta de origem, data e fatura. O saldo calculado das contas passa a incluir os pagamentos. Definir o tratamento de pagamento maior que a fatura e de pagamento de fatura ainda aberta.
- **Edição e exclusão em fatura paga:** bloquear compra ou parcela de fatura já paga, com erro claro.

## 3. Bloco D3: parcelas (opcional)
- A compra parcelada cria N transações, uma por parcela, ligadas por um mesmo plano. Cada parcela tem a data do mês dela, então cai na fatura certa e nos gastos do mês certo.
- Só para conta de cartão, de 2 a 24 parcelas.
- Valor da parcela: total dividido por N, com a sobra de centavos nas primeiras parcelas; a soma é sempre igual ao total (testado).
- Edição: categoria, descrição e estabelecimento mudam em todas as parcelas. Valor, data e número de parcelas não mudam: exclui o plano inteiro e cria de novo.
- ADR 017.

## 4. Cashback (início da Fase 3)
Meu caso: um cartão, 0,8% nas compras normais e 5% nas assinaturas (hoje Netflix e Spotify), sem teto mensal, creditado em conta ou Pix.
- **Regras por cartão:** percentual padrão e regras por categoria (5% em "Assinaturas"), com percentual guardado como `NUMERIC`.
- **Quando conta:** o cashback cai no dia da compra, então a estimativa é por mês da data da compra, sem depender da fatura.
- **Arredondamento:** cada compra, meio para cima, em duas casas; a soma é o total. Se a comparação mostrar diferença com o banco, ajustar.
- **Estimativa:** calculada no backend por mês e por cartão.
- **Recebido:** registro próprio (`cashback_receipts`) com cartão, conta de destino, data e valor. Aumenta o saldo da conta de destino e fica fora de gastos e renda.
- **Comparação:** estimado, recebido e diferença por mês; com dois ou três meses de dados, calibro as regras.
- **Parcelas:** cada parcela conta como uma compra, na data dela.
- Cada endpoint novo tem teste de acesso cruzado. ADR próprio na Fase 3.

## 5. Verificação
Mesmo padrão dos blocos anteriores: `./mvnw -B clean verify` contra o `fintrack_test`. Testes de fechamento no dia exato e no dia seguinte, virada de ano, soma das parcelas igual ao total, limite com parcelas futuras, pagamento parcial e acesso cruzado em todo endpoint novo.
