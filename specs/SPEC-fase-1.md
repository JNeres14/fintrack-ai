# SPEC — Fase 1: Planejamento e estruturação

> Registro da fase concluída. Parte do `SPEC.md` (resumo geral). Executada em 06 e 07/11/2026 pelo Claude Code (Sonnet 5.5), a partir de `prompts/prompt-fase-1.md`.
> Status: **concluída e aprovada em 07/11/2026** (documento técnico aprovado por mim; checkboxes da Fase 1 marcados em `docs/ROADMAP.md`).

## 1. Objetivo
Deixar o repositório estruturado, documentado e pronto para a Fase 2, com tudo versionado em Git. Sem regra de negócio, sem entidades, sem endpoints e sem frontend.

## 2. O que foi entregue
1. Repositório Git (branch `main`, 11 commits locais, sem remoto) com a estrutura de pastas: `frontend/`, `backend/`, `infrastructure/docker/`, `infrastructure/monitoring/`, `docs/`, `scripts/`, `.github/workflows/`, `prompts/`.
2. `.gitignore` (Java/Maven, Node, IDEs, `.env`) e `.env.example` com os nomes das variáveis, sem valores.
3. Documento técnico em `docs/documento-tecnico.md`, dividido por assunto em `docs/requirements/`, `docs/architecture/` e `docs/database/` (o original foi mantido). Conferência: 28 RF, 19 RNF, 9 UC e 124 linhas de tabela iguais ao original.
4. ADRs 001 a 008 em `docs/architecture/adr/`.
5. `README.md` inicial e `docs/ROADMAP.md` com as 7 fases.
6. Esqueleto do backend em `backend/`: Spring Boot 4.1.2, Java 25, Maven Wrapper, pacote base `com.fintrack`, pacotes vazios dos módulos (com `package-info.java`) e um teste de contexto.
7. `infrastructure/docker/docker-compose.yml` só com o PostgreSQL 17 (volume nomeado, variáveis do `.env`, healthcheck).
8. `.github/workflows/backend-ci.yml`: compila e testa o backend em push e pull request (Temurin 25).
9. Lista das telas do MVP em `docs/prototypes/telas-mvp.md`, sem desenho de interface.
10. `CLAUDE.md` (regras e comandos verificados), `AGENTS.md` e `STATUS.md` do projeto.

## 3. Verificação
**Verificado de fato**
- `./mvnw -B verify` terminou com BUILD SUCCESS e 1 teste passando.
- O PostgreSQL 17 subiu pelo Compose e ficou `healthy` (o Docker Desktop precisou ser iniciado; o contêiner foi derrubado depois e o volume foi mantido).

**Não verificado**
- O workflow do GitHub Actions nunca rodou, porque não há remoto.
- O teste de contexto roda sem banco: DataSource, JPA e Flyway estão excluídos em `backend/src/test/resources/application.properties`. Ele não valida a conexão com o PostgreSQL. A Fase 2 troca isso por Testcontainers.

## 4. Decisões e desvios em relação ao combinado
| Item | O que ficou | Por quê |
|---|---|---|
| Versão do Java | Java 25 (ADR-008) em vez do 21 suposto no prompt | Só o JDK 25 estava instalado |
| Versão do Spring Boot | 4.1.2 | O gerador do start.spring.io produziu `4.1.2.RELEASE`, que não existe no Maven Central; corrigido para `4.1.2` |
| Build | Maven Wrapper dentro de `backend/` | Maven não estava instalado |
| DTOs | `record`, sem Lombok | Decisão do Code; mantida |
| Identidade Git | Local ao repositório | Sem identidade global configurada |
| `.env` local | Existe com senha aleatória descartável, no `.gitignore` | Necessário para testar o Compose |
| `.gitkeep` | Restam em `infrastructure/docker` e `.github/workflows` | A remoção foi bloqueada por falso positivo da verificação de segurança; são inofensivos |
| Compose e CI | Escritos pelo Code | Anterior à regra de modo tutor; são ponto de partida e serão reescritos por mim na Fase 6 |

## 5. Critérios de aceite (ROADMAP)
- [x] Documento técnico aprovado por mim (07/11/2026)
- [x] README e ADRs iniciais existem
- [x] Telas principais listadas (sem protótipo visual)

## 6. Pendências ao encerrar a Fase 1
- Dúvidas em aberto do documento: API multimodal de IA, hospedagem e armazenamento de imagens, PDF no MVP, moeda única (BRL).
- Semgrep e Trivy não entraram no CI (ficam para as semanas 9 e 10 do `PLANO.md`).

## 7. Antes do primeiro push ao GitHub
Checagens feitas em 07/11/2026 no histórico local: nenhum remoto configurado, o `.env` nunca foi versionado e está protegido pelo `.gitignore`, e não há chaves ou tokens reais no histórico. Repetir a checagem se o histórico mudar antes do push.
- Criar o repositório no GitHub e conferir o e-mail de autoria dos commits.
- Conferir que `.env` continua fora do Git e que o `.env.example` não tem valores reais.
- Depois do push, conferir o primeiro resultado do workflow `Backend CI` (ainda não validado).
