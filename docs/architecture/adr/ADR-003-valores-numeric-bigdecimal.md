# ADR-003: Valores monetários em NUMERIC e BigDecimal

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
O sistema trata dinheiro; erros de arredondamento de ponto flutuante são inaceitáveis (RNF-10).

## Decisão
NUMERIC(14,2) no banco e BigDecimal no Java. amount é sempre positivo (CHECK amount > 0); o sinal vem do tipo da transação.

## Consequências
Precisão financeira. Exige teste de arredondamento e revisão de código para impedir float/double.

## Alternativa descartada
FLOAT/DOUBLE.
