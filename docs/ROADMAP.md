# Roadmap

Sete fases em sequência, com total estimado de 8 a 12 semanas (algumas horas por semana). Cada fase termina com algo que roda e pode ser verificado antes de seguir; a Fase 4 é o diferencial do produto. Fonte: `docs/documento-tecnico.md`, seção 9.

## Fase 1 · Planejamento
- [ ] Escopo: requisitos, repositório, arquitetura, modelo de dados, protótipos de tela
- [ ] Aceite: documento aprovado; README e ADRs iniciais; protótipos das telas principais

## Fase 2 · Backend e banco
- [ ] Escopo: Spring Boot, PostgreSQL + Flyway, entidades, API REST, autenticação e autorização, testes
- [ ] Aceite: migrações rodam do zero; CRUD de transações pela API; teste de isolamento entre usuários passa; CI verde

## Fase 3 · Frontend e dashboard
- [ ] Escopo: React + TypeScript, interface responsiva, dashboard, histórico, filtros, integração
- [ ] Aceite: login, lançamento manual e dashboard funcionam no celular e no PC

## Fase 4 · Integração com IA
- [ ] Escopo: API multimodal, upload, extração estruturada, revisão, duplicidade
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
