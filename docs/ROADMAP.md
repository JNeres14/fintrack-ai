# Roadmap

Sete fases em sequência, com total estimado de 8 a 12 semanas (algumas horas por semana). Cada fase termina com algo que roda e pode ser verificado antes de seguir; a Fase 4 é o diferencial do produto. Fonte: `docs/documento-tecnico.md`, seção 9.

## Fase 1 · Planejamento
- [x] Escopo: requisitos, repositório, arquitetura, modelo de dados, protótipos de tela (lista de telas, sem desenho)
- [x] Aceite: documento aprovado (07/11/2026); README e ADRs iniciais; telas principais listadas

## Fase 2 · Backend e banco
- [x] Escopo: Spring Boot, PostgreSQL + Flyway, entidades, API REST, autenticação e autorização, testes
- [x] Aceite: migrações rodam do zero; CRUD de transações pela API; teste de isolamento entre usuários passa; suíte verde localmente (CI fica para as semanas 9 e 10 do PLANO). Concluída em 11/11/2026: 164 testes, 0 falhas; inclui cartão/fatura (D2) e orçamentos (E); parcelas, cashback e ajustes de API ficam na Fase 3.

## Fase 3 · Frontend e dashboard
- [ ] Escopo: React + TypeScript, interface responsiva, dashboard, histórico, filtros, integração
- [ ] Aceite: login, lançamento manual e dashboard funcionam no celular e no PC

## Fase 4 · Integração com IA
- [ ] Escopo: API multimodal, upload, extração estruturada, revisão, duplicidade (Decisão de 09/11: extração local por padrão; ver `specs/SPEC-decisoes-fase-4.md`.)
- [ ] Aceite: print de Pix vira transação só após confirmação; extrato com várias linhas funciona; duplicata é sinalizada

## Fase 5 · Assistente
- [ ] Escopo: chat, consultas financeiras, resumo mensal, recomendações, orçamentos e alertas
- [ ] Aceite: respostas coincidem com os números calculados pelo backend; alerta de orçamento dispara

## Fase 6 · DevOps e infra
- [ ] Escopo: Docker Compose, CI/CD, deploy, monitoramento, logs estruturados, backup
- [ ] Aceite: `docker compose up` sobe tudo; merge na `main` publica; restauração de backup testada

## Fase 7 · Finalização
- [ ] Escopo: testes de integração e segurança, UX, documentação da API, README, vídeo
- [ ] Aceite: checklist de segurança (seção 8 do documento) cumprido; README e demonstração em vídeo prontos
