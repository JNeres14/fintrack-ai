# FinTrack AI
## Objetivo
Gerenciador financeiro pessoal (web + PWA) com extração de comprovantes por IA; descrição completa no `CLAUDE.md`.
## Decisões
- Escopo, fases e decisões: `SPEC.md` (fonte de verdade) e a spec da fase atual em `specs/` (Fase 2: `specs/SPEC-fase-2.md`). Requisitos detalhados: `docs/documento-tecnico.md` e `docs/`. Em dúvida, o documento vence; se achar que está errado, parar e perguntar.
- Regras inegociáveis do domínio, estrutura e comandos: `CLAUDE.md` desta pasta.
- Roteiro das fases: `prompts/prompt-fase-1.md`, `prompts/prompt-fase-2.md`.
## Regras
- Atualizar o STATUS.md ao final (ele já é carregado pelo `CLAUDE.md`).
- Ler SPEC.md, a spec da fase atual (`specs/SPEC-fase-N.md`) e ../../PLANO.md (objetivos e modo de trabalho) antes de escrever código.
- Trabalhar uma fase por conversa e um bloco por vez, parando para minha revisão ao fim de cada bloco.
- Modo aprendizado: em Dockerfile, Compose, CI/CD, Terraform e scripts, não escrever a solução; dar dicas, revisar e fazer perguntas. Em código de aplicação pode gerar, explicando as decisões principais e quais arquivos mudaram e por quê.
- Mudou uma decisão ou o escopo: atualizar a SPEC.md (e criar ADR quando for decisão arquitetural).
## Ambiente e tecnologias
Conferido na Fase 1 (07/11/2026). Reconferir com `java -version`, `psql --version` e `git --version` antes de começar uma fase; não digitar versão de memória.
- Linguagem/runtime (versão fixa): Java 25 (JDK instalado na máquina, em `JAVA_HOME`)
- Framework (versão fixa): Spring Boot 4.1.2
- Banco de dados (versão fixa): PostgreSQL 17 (instalado direto no Windows na Fase 2; Docker só nas semanas 7 e 8 do PLANO)
- Build: Maven pelo Maven Wrapper (`backend/mvnw`)
- Frontend (Fase 3): Node e versões do React, TypeScript e Vite a confirmar no início da fase
## Tecnologias permitidas
- Backend: Java, Spring Boot (Web MVC, Security, Data JPA, Validation, Actuator), Flyway, PostgreSQL, driver JDBC do PostgreSQL.
- Testes (backend): JUnit 5, Mockito, Spring Boot Test, contra o PostgreSQL local (banco `fintrack_test`).
- Fase 2: JWT pelo starter oauth2-resource-server (decidido, ADR-010); springdoc-openapi apenas no perfil `dev` (a confirmar com decisão registrada).
- Frontend (Fase 3): React, TypeScript, Tailwind CSS, Vite PWA, Vitest.
- Infra: nada na Fase 2. Docker/Compose (semanas 7 e 8), GitHub Actions (semanas 9 e 10), Prometheus e Grafana (Fase 6) entram quando o PLANO chegar lá.
- IA (Fase 4): extração local por padrão (OCR com regras primeiro, modelo de visão local como plano B). A API multimodal é só opcional, desligada por padrão e ativada por configuração. Tudo atrás das interfaces `VisionExtractor` e `FinancialAdvisor`; ver `specs/SPEC-decisoes-fase-4.md`.
- Decisão de 11/11 (Fase 4 e hospedagem): imagem do print é apagada ao confirmar o rascunho; backend e banco ficam no PC com Tailscale; ver `specs/SPEC-decisoes-rodada-2026-11-11.md`.
## Tecnologias proibidas / fora do escopo
- Microsserviços, Kubernetes, Kafka.
- App nativo Android/iOS, Open Finance, sincronização de cartões, controle de investimentos.
- AWS e Terraform antes da Fase 6.
- Docker, Compose, Testcontainers e CI na Fase 2 (os arquivos da Fase 1 ficam parados).
- Lombok (DTOs como `record`).
- `ddl-auto` create/update ou qualquer alteração de schema fora de migração Flyway.
- Semgrep, Trivy e Dependabot antes das semanas 9 e 10 do PLANO (Fase 2 não inclui).
- Qualquer biblioteca, serviço pago ou ferramenta fora da lista de permitidas, sem minha aprovação.
## Regras de ambiente
- Não mudar versão de linguagem, framework ou banco sem me perguntar.
- Não adicionar dependência, biblioteca ou serviço que não esteja em "Tecnologias permitidas".
- Se algo proibido parecer necessário, parar, explicar o motivo e esperar minha decisão.
- Se algo permitido não estiver instalado ou não funcionar, parar e avisar em vez de trocar por outra coisa.
- Toda divergência aprovada vira um ADR (ou nota no STATUS.md).
- Não alterar `infrastructure/docker/docker-compose.yml` nem `.github/workflows/backend-ci.yml` sem pedir (modo tutor; ver SPEC.md, seção 6).

## Preparo da tarefa
- Ler PREPARO.md antes de começar (modelo, skills e pastas decididos no chat). Se estiver vazio ou desatualizado, avisar em vez de supor.
- Se o PREPARO.md mandar usar uma skill que não esteja instalada, parar e avisar.
- Ao fim da sessão, no STATUS.md, acrescentar: "Skills usadas: X. Não usadas: Y, porque Z."
