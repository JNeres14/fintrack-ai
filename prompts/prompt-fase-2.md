# Prompt — Fase 2 (Backend e banco de dados)

Cole o texto abaixo no Claude Code, na raiz do `fintrack-ai`, depois que a Fase 1 estiver concluída e commitada. Se o Code criou algo diferente do esperado na Fase 1 (versão do Java, Gradle em vez de Maven, nomes de pastas), ajuste os pontos marcados com `[confirmar]` antes de colar.

---

Vamos começar a Fase 2 do FinTrack AI (backend e banco de dados). O objetivo é uma **API financeira funcional, segura e testada**, sem frontend e sem IA ainda.

Leia primeiro o `CLAUDE.md`, `docs/ROADMAP.md` e o documento técnico em `docs/`, principalmente as seções 2 (requisitos), 3 (não funcionais), 5 (arquitetura), 6 (modelo de dados) e 8 (segurança). Confira também o estado atual do repositório (`git log`, árvore de pastas) antes de planejar.

## Escopo da Fase 2

Entra: usuários e autenticação, contas, categorias, formas de pagamento, transações (com filtros), orçamentos, testes automatizados e CI.

Não entra (ficam para fases posteriores): upload de imagem, `Attachment`, `AIAnalysis`, qualquer chamada a IA, `Goal`, `RecurringTransaction`, dashboard/relatórios e frontend. A coluna `attachment_id` de `Transaction` será adicionada junto com `Attachment` na Fase 4, por nova migração. Diga se discorda dessa decisão.

## Como trabalhar

Divida o trabalho nos blocos A a E abaixo. **Para cada bloco:** mostre um plano curto (arquivos e decisões), espere meu "ok", implemente com testes, rode a verificação, faça commits pequenos em português no imperativo, apresente um relatório curto (o que foi feito, o que foi verificado de fato, o que ficou pendente) e **pare até eu dizer "continuar"**. Não avance para o próximo bloco por conta própria e não faça push.

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
5. Testes com Testcontainers (PostgreSQL real): as migrações rodam do zero e o contexto sobe.

## Bloco B — Autenticação e autorização (RF-01, RF-27)

1. Cadastro, login, refresh e logout em `auth`/`user`.
2. Senhas com BCrypt (ou Argon2, se justificar). Política mínima de senha, definida por você e explicada.
3. JWT de acesso com expiração curta (máx. 15 min) e refresh token com **rotação** (o refresh usado é invalidado). Refresh tokens guardados com hash no banco, nunca em texto puro.
4. Bloqueio temporário após tentativas de login falhas e rate limiting no login (RNF-13), com resposta 429 testada.
5. CORS restrito por configuração; segredos do JWT só em variáveis de ambiente, sem valor padrão inseguro no código.
6. Um componente central que fornece o `userId` do usuário autenticado ao restante do código. Nenhum endpoint aceita `userId` vindo do cliente.
7. Testes: cadastro/login felizes, credenciais erradas, token expirado, refresh reutilizado, bloqueio por tentativas, e confirmação de que nenhuma senha ou token aparece em log.

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

## Bloco E — Orçamentos, qualidade e CI (RF-20)

1. Orçamento mensal geral e por categoria, com consulta do consumo atual (soma calculada no backend via consulta SQL/JPA, nunca no cliente).
2. Documentação da API: OpenAPI (springdoc) acessível apenas no perfil `dev`.
3. Atualizar o workflow de CI para rodar a suíte completa (inclusive Testcontainers) em push e pull request, e adicionar uma varredura de segredos.
4. Atualizar `docs/ROADMAP.md` (checkboxes da Fase 2), `docs/api/` com os endpoints e o `CLAUDE.md` com os comandos reais de build/teste.
5. Registrar ADRs para decisões novas desta fase (por exemplo: estratégia de refresh token, política de exclusão, categorias padrão).

## Regras fixas (resumo do CLAUDE.md)

- Dinheiro sempre em `NUMERIC(14,2)` / `BigDecimal`.
- Toda consulta filtra pelo usuário autenticado; todo endpoint novo tem teste de acesso cruzado.
- DTOs na API, entidades JPA nunca expostas.
- Logs sem valores, descrições, estabelecimentos, tokens ou senhas.
- Banco só muda por migração Flyway; nunca editar migração já aplicada.
- Não inventar requisito: na dúvida, pergunte.

## Verificação ao final de cada bloco

Rode o build e a suíte de testes e mostre a saída real (testes executados, passaram, falharam). Se algo não puder ser executado no ambiente (por exemplo Docker para Testcontainers), diga claramente em vez de supor que funcionou.

## Critério de aceite da Fase 2

- As migrações rodam do zero e a suíte de testes está verde, local e no CI.
- É possível, só pela API (Swagger no perfil `dev`): cadastrar, logar, criar conta, categorias, lançar transações, filtrar o histórico e definir um orçamento.
- Os testes de isolamento entre usuários passam em todos os endpoints.
- Nenhum segredo no repositório; `.env.example` completo.

Comece listando as dúvidas do documento para esta fase e o plano do Bloco A. Não implemente nada antes do meu "ok".

---

## Antes de rodar

- **`[confirmar]` stack:** o prompt assume Maven, Java 21 e Spring Boot como definido na Fase 1. Se o Code usou outra coisa, ajuste.
- **Docker:** os testes com Testcontainers precisam do Docker Desktop ligado na sua máquina.
- **Decisões que ele vai propor e você deve validar:** política de senha, lista de categorias padrão, regra de exclusão de categoria/conta em uso e estratégia de refresh token.
- **Dashboard:** os endpoints de resumo (saldo, totais, comparação mensal) ficam para o início da Fase 3, junto com a tela que os usa.
