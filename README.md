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
Fase 1 (planejamento e estruturação) concluída. Fase 2 (backend e banco): API implementada (autenticação, contas, cartão e fatura, categorias, formas de pagamento, transações e orçamentos), aguardando o aceite final.

## Como rodar (backend, Fase 2)
Pré-requisitos: JDK 25 em `JAVA_HOME` e PostgreSQL 17 local com os bancos `fintrack` (desenvolvimento) e `fintrack_test` (testes). O Maven vem pelo wrapper.

1. Copie `.env.example` para `.env` (na raiz; fica fora do Git) e preencha: `DB_URL`, `DB_TEST_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` (mínimo 32 bytes aleatórios, por exemplo `openssl rand -base64 48`). `CORS_ALLOWED_ORIGINS` só é preciso com um frontend em outra origem.
2. Testes (apagam e recriam o schema do `fintrack_test`, nunca o `fintrack`):
   ```bash
   cd backend
   ./mvnw -B clean verify
   ```
3. Subir a API no perfil `dev` (aplica as migrações Flyway no banco `fintrack`):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
4. Swagger UI (só no `dev`): http://localhost:8080/swagger-ui.html. Cadastre em `POST /api/auth/register`, faça `POST /api/auth/login`, clique em **Authorize** e cole o `accessToken`.

Referência dos endpoints: `docs/api/README.md`.
