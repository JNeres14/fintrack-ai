# ADR-001: Monólito modular

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
Projeto pessoal, com poucas horas por semana e arquitetura que precisa servir a vários usuários no futuro. O documento busca menos complexidade operacional.

## Decisão
Um único deploy Spring Boot dividido em módulos com fronteiras claras (controller, service, repository; um módulo só usa outro pela interface pública).

## Consequências
Operação simples agora e evolução por módulos. Permite extrair um módulo no futuro, se necessário. Exige disciplina para não acoplar módulos pelos repositories.

## Alternativa descartada
Microsserviços (fora do escopo inicial, junto com Kubernetes).
