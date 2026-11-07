# SPEC — Fase 2: Backend e banco de dados

> Parte do `SPEC.md` (resumo geral). Prompt de execução: `prompts/prompt-fase-2.md`. Requisitos detalhados: `docs/`. Regras de ambiente, tecnologias permitidas e proibidas: `AGENTS.md`.
> Status: **pronta para executar**, depois que eu aprovar o documento técnico e commitar as specs.
> Se esta spec e o prompt divergirem, vale esta spec.

## 1. Objetivo
Uma API financeira funcional, segura e testada: cadastro e login, contas, categorias, formas de pagamento, transações com filtros e orçamentos, com testes automatizados contra um PostgreSQL real. Sem frontend e sem IA.

## 2. Ponto de partida (fim da Fase 1)
- Java 25, Spring Boot 4.1.2, Maven Wrapper, PostgreSQL 17. O Docker Compose da Fase 1 existe, mas não é usado nesta fase.
- Backend só com o esqueleto: pacotes vazios dos módulos e um teste de contexto sem banco (DataSource, JPA e Flyway excluídos em `backend/src/test/resources/application.properties`).
- O CI nunca rodou (não há remoto no GitHub).
- `.env` local com senha descartável, fora do Git.
- Fase 1 aguardando minha aprovação do documento técnico (ver `specs/SPEC-fase-1.md`).

**Antes de começar**
- PostgreSQL 17 instalado direto no Windows, com um usuário próprio e dois bancos: `fintrack` (desenvolvimento) e `fintrack_test` (testes). Eu instalo e crio; o Code não instala nada na máquina.
- Claude Code aberto em `Documents\PROJETOS\fintrack-ai`.
- Quando o Code atualizar o `.env.example` com as variáveis de conexão e de JWT, eu preencho o meu `.env` (o Code não lê o `.env`).

## 3. Escopo
**Entra:** usuários e autenticação, contas, categorias (com subcategorias), formas de pagamento, transações com filtros, orçamentos, testes automatizados, `/health`, OpenAPI no perfil `dev`.

**Não entra:**
- Upload de imagem, `Attachment` (e a coluna `attachment_id` de `Transaction`), `AIAnalysis`, qualquer chamada a IA: Fase 4.
- `Goal` e `RecurringTransaction`: depois do MVP (P2).
- Dashboard, relatórios e os endpoints de resumo: início da Fase 3.
- Frontend.
- Docker, Docker Compose, Testcontainers e CI/GitHub Actions: semanas 7 a 10 do `PLANO.md`. O Compose e o workflow da Fase 1 ficam parados; o Code não os usa nem altera.
- Semgrep, Trivy, Dependabot e varredura de segredos no CI: semanas 9 e 10 do `PLANO.md`.

## 4. Blocos
Um bloco por vez. Em cada um: plano curto → meu "ok" → implementação com testes → verificação real → relatório → pausa até eu dizer "continuar". O Code não commita.

### Bloco A — Base do banco e padrões da API
- Conexão com o PostgreSQL por variáveis de ambiente, perfis `dev` e `test`, `.env.example` atualizado.
- Migração Flyway `V1`: `users`, `accounts`, `categories`, `payment_methods`, `transactions`, `budgets`.
  - UUID como chave primária; `NUMERIC(14,2)`; `occurred_on DATE`; `created_at`/`updated_at` em `TIMESTAMPTZ`.
  - `user_id` em todas as tabelas de dados, com chaves estrangeiras e índices `(user_id, occurred_on)` e `(user_id, category_id)`.
  - `CHECK (amount > 0)`; `type` limitado a receita/despesa; `categories.parent_id` para subcategorias.
  - Sem `attachment_id`.
- Entidades JPA e repositories nos módulos certos; nenhuma entidade exposta na API.
- Em `shared`: erros padronizados (sem stack trace nem dados sensíveis), Bean Validation, `BigDecimal` no JSON.
- Testes de banco contra o PostgreSQL local, no banco `fintrack_test` (perfil `test`), no lugar das exclusões de autoconfiguração. As migrações rodam do zero nesse banco; qualquer limpeza (Flyway clean) só no perfil `test` e com trava para nunca apontar para o banco de desenvolvimento. O Code propõe a abordagem e espera meu "ok".
- **Pronto quando:** o contexto sobe com DataSource, JPA e Flyway ativos e as migrações rodam do zero.

### Bloco B — Autenticação e autorização (RF-01, RF-27, RNF-01 a 04 e 13)
- Cadastro, login, refresh e logout.
- Senha com hash seguro (BCrypt ou Argon2); política mínima de senha proposta pelo Code.
- JWT de acesso (máx. 15 min) e refresh token com rotação, guardado com hash no banco. **Antes de implementar**, o Code propõe a biblioteca/abordagem de JWT compatível com Spring Boot 4.1.2 e espera meu "ok".
- Bloqueio temporário por tentativas falhas e rate limiting no login com resposta 429, sem biblioteca nova sem aprovação.
- CORS restrito por configuração; segredo do JWT só em variável de ambiente, sem valor padrão inseguro.
- Componente central que fornece o `userId` autenticado; nenhum endpoint aceita `userId` do cliente.
- `GET /actuator/health` liberado sem autenticação (só o necessário do Actuator).
- **Testes:** credencial errada, token expirado, refresh reutilizado, bloqueio por tentativas, 429, e nenhuma senha ou token em log.

### Bloco C — Contas, categorias e formas de pagamento (RF-03, RF-04)
- CRUD de contas com saldo **calculado** (saldo inicial + receitas − despesas), não armazenado.
- CRUD de categorias com subcategorias; categorias padrão criadas para cada novo usuário (lista proposta pelo Code antes de implementar).
- CRUD de formas de pagamento, com padrão (Pix, débito, crédito, dinheiro).
- Nomes únicos por usuário; regra para excluir item em uso (proposta: bloquear).
- **Testes:** acesso cruzado em cada endpoint (usuário A não lê, altera nem apaga dados de B; resposta 404, não 403).

### Bloco D — Transações (RF-02, RF-05)
- CRUD com data, valor, tipo, descrição, estabelecimento, categoria, forma de pagamento e conta.
- Listagem paginada com filtros por período, categoria, tipo, conta e texto, ordenada por data decrescente.
- Validações: valor positivo com no máximo 2 casas, categoria/conta/forma de pagamento do mesmo usuário, data válida.
- **Testes:** precisão e arredondamento, filtros combinados, acesso cruzado (inclusive usar categoria de outro usuário).

### Bloco E — Orçamentos e fechamento (RF-20)
- Orçamento mensal geral e por categoria; consumo atual calculado no backend (SQL/JPA), nunca no cliente.
- OpenAPI (springdoc) só no perfil `dev`; confirmar antes a compatibilidade com Spring Boot 4.1.2.
- CI, Docker e varreduras de segurança: fora desta fase (semanas 7 a 10 do `PLANO.md`).
- Documentar a API em `docs/api/`, atualizar o `CLAUDE.md` (comandos reais), o `STATUS.md` e o README ("como rodar" a API); sugerir os checkboxes da Fase 2 no ROADMAP (eu confirmo).
- ADRs das decisões novas da fase (a partir do ADR-009).

## 5. Decisões a tomar (o Code propõe, eu aprovo; cada uma vira ADR)
| Decisão | Proposta inicial |
|---|---|
| Política de senha | A definir pelo Code, com justificativa |
| Biblioteca/abordagem de JWT | A definir pelo Code (compatível com Spring Boot 4.1.2), com prós e contras |
| Refresh token | Rotação, com hash no banco |
| Excluir conta/categoria em uso | Bloquear |
| Categorias e formas de pagamento padrão | O Code lista antes de implementar |
| Paginação padrão | 20 por página, máximo 100 |
| Moeda | BRL único no MVP, sem coluna de moeda (confirmar) |
| Rate limiting | Implementação simples em memória; limitação aceita para uso pessoal |

## 6. Modo de trabalho nesta fase
- Código da aplicação: acelerador; autenticação com revisão reforçada (o Code explica cada decisão).
- Docker, Compose e CI: fora desta fase; o Code não os altera.
- Sem commit pelo Code: ao fim de cada bloco ele lista os arquivos e sugere a mensagem; eu reviso e commito.
- Ao fim de cada bloco, `/clear` e o `STATUS.md` como passagem de contexto.
- Sugestões ainda **não aprovadas** (decidir antes de usar): sonnet com esforço medium e `opusplan` no Bloco B; Context7 para a documentação do Spring Boot 4.1; subagente barato para rodar `./mvnw verify`; skill `revisao-de-bloco` ao fim de cada bloco. Se aprovadas, entram na lista de permitidas do `AGENTS.md`.

## 7. Riscos
- Compatibilidade do springdoc e da biblioteca de JWT com Spring Boot 4.1.2 e Java 25.
- Testes que escrevem no banco local: apontar por engano para `fintrack` em vez de `fintrack_test` apagaria dados de desenvolvimento; por isso a trava do Bloco A.
- Crescimento de escopo (dashboard e relatórios ficam para a Fase 3 em diante).
- Gerar sem entender: se eu não consigo explicar um arquivo, o bloco não está pronto.

## 8. Aceite da Fase 2
- [ ] Migrações rodam do zero.
- [ ] `./mvnw -B verify` verde localmente, contra o banco `fintrack_test` e sem as exclusões de autoconfiguração.
- [ ] Pela API (Swagger no perfil `dev`): cadastrar, logar, criar conta e categorias, lançar transações, filtrar o histórico e definir um orçamento.
- [ ] Testes de isolamento entre usuários em todos os endpoints.
- [ ] `/health` respondendo sem autenticação.
- [ ] Nenhum segredo no repositório; `.env.example` completo.
- [ ] README com "como rodar" a API; `docs/api/` e ADRs atualizados.
- [ ] Pendência registrada: CI e Testcontainers entram depois do Docker (semanas 7 a 10 do `PLANO.md`).

## 9. Como testar ao final
1. Conferir que o PostgreSQL 17 local está rodando e que os bancos `fintrack` e `fintrack_test` existem.
2. Rodar `./mvnw -B verify` em `backend/` e conferir testes executados, passados e falhos.
3. Subir a API no perfil `dev` (comando documentado no README pelo Code) e percorrer o fluxo pelo Swagger.
4. Tentar acessar dados de outro usuário e conferir que a resposta é 404.

## 10. Encerramento
- O Code atualiza o `STATUS.md`; eu reviso e commito.
- Eu marco a Fase 2 no `SPEC.md` e no `docs/ROADMAP.md`.
- Proposta: marcar a fase com uma tag Git (`fase-2`), como pede o `PLANO.md` para cada camada.
