# Status – FinTrack AI
- Feito (Fase 1, tarefas 1 a 9): repositório Git (branch main, 10 commits locais, sem push); estrutura de pastas; .gitignore e .env.example; documento técnico dividido em docs/requirements, docs/architecture e docs/database (original mantido); ADRs 001 a 008; README e ROADMAP; esqueleto Spring Boot 4.1.2 / Java 25 em backend/ com pacotes dos módulos e teste de contexto; docker-compose com PostgreSQL 17; workflow backend-ci.yml; telas do MVP em docs/prototypes.
- Verificado de fato: `./mvnw -B verify` com 1 teste passando (BUILD SUCCESS); PostgreSQL subiu pelo compose e ficou (healthy).
- Não verificado: o workflow do GitHub Actions (nunca rodou, não há remoto); o teste de contexto roda sem banco (DataSource, JPA e Flyway excluídos em src/test/resources/application.properties), então ainda não valida a conexão com o Postgres.
- Em andamento: nada. Aguardando revisão do usuário antes da Fase 2.
- Próximos passos: revisar a Fase 1 e marcar os checkboxes do ROADMAP; decidir sobre Semgrep/Trivy no CI; Fase 2 (prompts/prompt-fase-2.md), começando pelas dúvidas do documento e pelo plano do Bloco A. Na Fase 2, trocar a exclusão de autoconfiguração por Testcontainers.
- Decisões: Java 25 e Spring Boot 4.1.2 (ADR-008, diverge do Java 21 suposto no prompt); Maven Wrapper; sem Lombok (DTOs como record); identidade Git local "Jose Neres" com joseneres14@gmail.com.
- Dúvidas em aberto (do documento): API multimodal de IA, hospedagem e armazenamento de imagens, PDF no MVP, moeda única (BRL).
- Última sessão: Claude Code / Sonnet 5.5, 2026-11-07. Pendente: revisão do usuário e Fase 2.
