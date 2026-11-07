# ADR-006: Provedor de IA atrás de interface

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
O provedor de IA ainda não foi escolhido (questão em aberto do documento) e pode mudar por custo, retenção de dados ou qualidade.

## Decisão
O código usa as interfaces VisionExtractor e FinancialAdvisor; cada provedor é uma implementação, e há implementações fake para testes.

## Consequências
Troca de provedor sem mexer na regra de negócio e testes sem chamar a API. Exige manter o contrato de saída (schema) independente do provedor.

## Alternativa descartada
Acoplar o código ao SDK de um provedor.
