# FinTrack AI

Gerenciador financeiro pessoal (web responsiva + PWA). O usuário envia imagens de comprovantes, a IA extrai as transações, o usuário revisa e confirma. Uso pessoal hoje, com arquitetura pronta para múltiplos usuários.

## Stack
- Backend: Java + Spring Boot (monólito modular), Spring Security, PostgreSQL, Flyway
- Frontend: React + TypeScript + Tailwind + Vite PWA
- Testes: JUnit + Mockito (backend), Vitest (frontend)
- Infra: Docker/Docker Compose, GitHub Actions, Prometheus + Grafana

## Documentação
- `docs/documento-tecnico.md`: documento técnico completo (referência única)
- `docs/requirements/`: visão, requisitos funcionais e não funcionais, casos de uso
- `docs/architecture/`: arquitetura, módulo de IA, segurança, decisões; ADRs em `docs/architecture/adr/`
- `docs/database/`: modelo de dados
- `docs/prototypes/`: telas do MVP
- `docs/ROADMAP.md`: as 7 fases com critérios de aceite
- `SPEC.md` e `specs/`: escopo geral e spec de cada fase
- `CLAUDE.md`: regras de trabalho do projeto

## Status
Fase 1 (planejamento e estruturação) concluída. Fase 2 (backend e banco) é a próxima. Sem regra de negócio implementada.

## Como rodar
A definir na Fase 2.
