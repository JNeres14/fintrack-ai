# ADR-004: Cálculos no backend; a IA só interpreta

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
A IA pode errar contas e não é verificável. O assistente precisa de respostas que coincidam com os números reais.

## Decisão
Todo cálculo financeiro é feito no backend (SQL/serviços). A IA recebe apenas resultados agregados e os explica, sem recalcular.

## Consequências
Resultados verificáveis e testáveis; o assistente recebe só os dados necessários (RNF-07). O backend precisa implementar cada consulta que o assistente usa.

## Alternativa descartada
IA calculando a partir do histórico.
