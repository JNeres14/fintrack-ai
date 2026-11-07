# Prompt — Fase 2 (Backend e banco de dados)

Cole o texto abaixo no Claude Code aberto dentro de `Documents\PROJETOS\fintrack-ai` (a pasta do repositório da Fase 1). Esta versão já considera o resultado real da Fase 1: Java 25, Spring Boot 4.1.2, Maven Wrapper, PostgreSQL 17 e teste de contexto sem banco. O Docker Compose e o CI da Fase 1 existem, mas ficam fora desta fase.

---

Vamos começar a Fase 2 do FinTrack AI (backend e banco de dados). O objetivo é uma **API financeira funcional, segura e testada**, sem frontend e sem IA ainda.

Leia primeiro o `AGENTS.md`, o `CLAUDE.md`, o `STATUS.md`, o `SPEC.md`, o `specs/SPEC-fase-2.md` (escopo, blocos, decisões, riscos e aceite desta fase), o `../../PLANO.md`, `docs/ROADMAP.md`, o ADR-008 e o documento técnico em `docs/`, principalmente as seções 2 (requisitos), 3 (não funcionais), 5 (arquitetura), 6 (modelo de dados) e 8 (segurança). Confira também o estado atual do repositório (`git log`, árvore de pastas) antes de planejar. Se este prompt divergir do `specs/SPEC-fase-2.md`, do `SPEC.md` ou do `AGENTS.md`, valem os specs e o `AGENTS.md`; me avise da divergência.

## Ponto de partida (resultado da Fase 1)

- Stack real: Java 25, Spring Boot 4.1.2, Maven Wrapper (`./mvnw`), PostgreSQL 17 instalado direto no Windows, com os bancos `fintrack` (desenvolvimento) e `fintrack_test` (testes). Mantenha essa stack; não volte para o Java 21 sem me perguntar.
- O teste de contexto atual exclui DataSource, JPA e Flyway em `backend/src/test/resources/application.properties`, ou seja, não valida o banco. Nesta fase isso deve ser **substituído** por testes contra o PostgreSQL local (banco `fintrack_test`), removendo essas exclusões.
- Docker, Docker Compose, Testcontainers e CI (GitHub Actions) são assuntos das semanas 7 a 10 do `PLANO.md`: não use nem configure nada disso e não altere `docker-compose.yml` nem `backend-ci.yml`. Se algo parecer exigir isso, pare e me pergunte.
- O `.env` local contém uma senha descartável e está fora do Git. Não leia nem exiba o conteúdo; use apenas o `.env.example` como referência.
- Os critérios de aceite da Fase 1 no ROADMAP seguem sem marcar porque dependem da minha aprovação do documento. Não marque nada da Fase 1.
- Semgrep, Trivy e Dependabot ficam fora da Fase 2 (semanas 9 e 10 do `PLANO.md`).
- A verificação de segurança do ambiente já bloqueou comandos com `rm` por falso positivo. Evite remoções; se uma for realmente necessária, avise e me diga o que seria removido.

## Escopo da Fase 2

Entra: usuários e autenticação, contas, categorias, formas de pagamento, transações (com filtros), orçamentos, testes automatizados, `/health` e OpenAPI no perfil dev. Docker, Compose, Testcontainers e CI ficam fora (semanas 7 a 10 do PLANO).

Não entra (ficam para fases posteriores): upload de imagem, `Attachment`, `AIAnalysis`, qualquer chamada a IA, `Goal`, `RecurringTransaction`, dashboard/relatórios e frontend. A coluna `attachment_id` de `Transaction` será adicionada junto com `Attachment` na Fase 4, por nova migração. Diga se discorda dessa decisão.

## Como trabalhar

Divida o trabalho nos blocos A a E abaixo. **Para cada bloco:** mostre um plano curto (arquivos e decisões), espere meu "ok", implemente com testes, rode a verificação, **não faça commit** (liste os arquivos alterados e sugira a mensagem de commit), apresente um relatório curto (o que foi feito, o que foi verificado de fato, o que ficou pendente, as decisões principais e quais arquivos mudaram e por quê) e **pare até eu dizer "continuar"**. Não avance para o próximo bloco por conta própria e não faça push.

Antes do bloco A, liste em poucas linhas as dúvidas ou ambiguidades do documento que impactam esta fase e proponha uma decisão padrão para cada uma.

## Bloco A — Base do banco e padrões da API

1. Configurar o Spring Boot para ler a conexão do PostgreSQL por variáveis de ambiente (`.env.example` atualizado), com perfis `dev` e `test`.
2. Flyway: migração `V1` criando `users`, `accounts`, `categories`, `payment_methods`, `transactions` e `budgets`, conforme a seção 6 do documento:
   - UUID como chave primária, `NUMERIC(14,2)` para valores, `occurred_on DATE`, `created_at`/`updated_at` em `TIMESTAMPTZ`.
   - `user_id` em todas as tabelas de dados, com chaves estrangeiras e os índices `(user_id, occurred_on)` e `(user_id, category_id)`.
   - `CHECK (amount > 0)` e `type` limitado a receita/despesa; `categories.parent_id` para subcategorias.
   - Sem `attachment_id` por enquanto.
3. Entidades JPA e repositories correspondentes, nos módulos certos (`user`, `transaction`, `category`, `budget`). Nada de entidade exposta na API.
4. Padrões compartilhados em `shared`: tratamento global de erros com resposta padronizada (sem vazar stack trace nem dados sensíveis), validação com Bean Validation, tipo/serialização de dinheiro (`BigDecimal` em JSON, sem `double`).
5. Testes de banco contra o PostgreSQL local, no banco `fintrack_test` (perfil `test`): as migrações rodam do zero e o contexto sobe com DataSource, JPA e Flyway ativos. Proponha a abordagem antes (por exemplo, limpeza do schema só no perfil `test`, com trava para nunca apontar para o banco `fintrack`) e espere meu "ok". Se o PostgreSQL local não estiver acessível, pare e me avise em vez de contornar.

## Bloco B — Autenticação e autorização (RF-01, RF-27)

1. Cadastro, login, refresh e logout em `auth`/`user`.
2. Senhas com BCrypt (ou Argon2, se justificar). Política mínima de senha, definida por você e explicada.
3. Antes de implementar, proponha a biblioteca/abordagem de JWT (compatível com Spring Boot 4.1.2, com prós e contras) e espere meu "ok"; registre a escolha em ADR. JWT de acesso com expiração curta (máx. 15 min) e refresh token com **rotação** (o refresh usado é invalidado). Refresh tokens guardados com hash no banco, nunca em texto puro.
4. Bloqueio temporário após tentativas de login falhas e rate limiting no login (RNF-13), com resposta 429 testada. Sem adicionar biblioteca nova sem me perguntar (uma implementação simples em memória serve para uso pessoal; diga as limitações).
5. CORS restrito por configuração; segredos do JWT só em variáveis de ambiente, sem valor padrão inseguro no código.
6. Um componente central que fornece o `userId` do usuário autenticado ao restante do código. Nenhum endpoint aceita `userId` vindo do cliente.
7. Testes: cadastro/login felizes, credenciais erradas, token expirado, refresh reutilizado, bloqueio por tentativas, e confirmação de que nenhuma senha ou token aparece em log.
8. Liberar `GET /actuator/health` sem autenticação (e só o necessário do Actuator), com teste.

## Bloco C — Contas, categorias e formas de pagamento (RF-03, RF-04)

1. CRUD de contas com saldo **calculado** (saldo inicial + receitas − despesas), não armazenado.
2. CRUD de categorias com subcategorias. Um conjunto padrão de categorias (alimentação, transporte, moradia, saúde, lazer, educação, outros etc.) criado automaticamente para cada novo usuário; proponha a lista antes de implementar.
3. CRUD de formas de pagamento, com um conjunto padrão (Pix, débito, crédito, dinheiro) criado por usuário.
4. Regras: não excluir categoria/conta em uso sem tratamento definido (proponha a regra: bloquear ou reatribuir) e nomes únicos por usuário.
5. Testes de **acesso cruzado** para cada endpoint: o usuário A não lê, altera nem apaga dados do usuário B (resposta 404, não 403, para não revelar existência).

## Bloco D — Transações (RF-02, RF-05)

1. CRUD de transações com data, valor, tipo, descrição, estabelecimento, categoria, forma de pagamento e conta.
2. Listagem paginada com filtros por período, categoria, tipo, conta e texto (descrição/estabelecimento), ordenada por data desc.
3. Validações: valor positivo com no máximo 2 casas decimais, categoria/conta/forma de pagamento pertencentes ao mesmo usuário, data válida.
4. Testes de arredondamento e precisão monetária, de filtros combinados e de acesso cruzado (inclusive tentar usar categoria de outro usuário numa transação).

## Bloco E — Orçamentos e documentação (RF-20)

1. Orçamento mensal geral e por categoria, com consulta do consumo atual (soma calculada no backend via consulta SQL/JPA, nunca no cliente).
2. Documentação da API: OpenAPI (springdoc) acessível apenas no perfil `dev`. Confirme antes que o springdoc é compatível com Spring Boot 4.1.2; se não for, me diga as opções.
3. CI, Docker e varreduras de segurança: fora desta fase (semanas 7 a 10 do PLANO).
4. Sugerir os checkboxes da Fase 2 no `docs/ROADMAP.md` (eu confirmo) e atualizar `docs/api/` com os endpoints, o `CLAUDE.md` com os comandos reais de build/teste, o README ("como rodar" a API) e o `STATUS.md`.
5. Registrar ADRs para decisões novas desta fase (por exemplo: estratégia de refresh token, política de exclusão, categorias padrão).

## Regras fixas (resumo do CLAUDE.md)

- Dinheiro sempre em `NUMERIC(14,2)` / `BigDecimal`.
- Toda consulta filtra pelo usuário autenticado; todo endpoint novo tem teste de acesso cruzado.
- DTOs na API, entidades JPA nunca expostas.
- Logs sem valores, descrições, estabelecimentos, tokens ou senhas.
- Banco só muda por migração Flyway; nunca editar migração já aplicada.
- Não inventar requisito: na dúvida, pergunte.

## Verificação ao final de cada bloco

Rode o build e a suíte de testes e mostre a saída real (testes executados, passaram, falharam). Se algo não puder ser executado no ambiente (por exemplo, o PostgreSQL local desligado), diga claramente em vez de supor que funcionou.

## Critério de aceite da Fase 2

- As migrações rodam do zero e a suíte de testes está verde localmente (`./mvnw -B verify`).
- É possível, só pela API (Swagger no perfil `dev`): cadastrar, logar, criar conta, categorias, lançar transações, filtrar o histórico e definir um orçamento.
- Os testes de isolamento entre usuários passam em todos os endpoints.
- Nenhum segredo no repositório; `.env.example` completo.

Comece listando as dúvidas do documento para esta fase e o plano do Bloco A. Não implemente nada antes do meu "ok".

---

## Antes de rodar

- **Pasta certa:** abra o Claude Code em `Documents\PROJETOS\fintrack-ai`.
- **PostgreSQL:** instale o PostgreSQL 17 no Windows, crie um usuário para o projeto e os bancos `fintrack` e `fintrack_test` (senha só no seu `.env`). Sem isso o Bloco A não consegue verificar nada.
- **`.env`:** quando o Code atualizar o `.env.example`, preencha o seu `.env` com as variáveis de conexão do backend (o Code não lê o `.env`).
- **Java 25:** a Fase 1 usou o JDK 25 por ser o único instalado (ADR-008). Se preferir o 21, instale-o e peça a troca antes do Bloco A.
- **Decisões que ele vai propor e você deve validar:** política de senha, lista de categorias padrão, regra de exclusão de categoria/conta em uso e estratégia de refresh token.
- **Dashboard:** os endpoints de resumo (saldo, totais, comparação mensal) ficam para o início da Fase 3, junto com a tela que os usa.
