@AGENTS.md
@STATUS.md
@PREPARO.md

# FinTrack AI

Gerenciador financeiro pessoal (web + PWA). O usuário envia imagens de comprovantes, a IA extrai as transações, o usuário revisa e confirma. Uso pessoal hoje, arquitetura pronta para múltiplos usuários.

Escopo e fases: `SPEC.md` e a spec da fase atual em `specs/` (Fase 2: `specs/SPEC-fase-2.md`). Requisitos e referência principal: `docs/` (documento técnico inicial com requisitos RF/RNF, casos de uso, arquitetura, modelo ER, módulo de IA, segurança e roadmap). Em dúvida, o documento vence; se achar que ele está errado, pare e pergunte antes de divergir.

## Stack

- Backend: Java + Spring Boot (monólito modular), Spring Security, PostgreSQL, Flyway, Maven Wrapper (versões fixas em `AGENTS.md`)
- Frontend: React + TypeScript + Tailwind + Vite PWA
- Testes: JUnit + Mockito (backend), Vitest (frontend)
- Infra: Docker/Compose, GitHub Actions, Prometheus + Grafana (fora da Fase 2; ver `AGENTS.md`)

## Estrutura

```
fintrack-ai/
├── frontend/
├── backend/            pacote base: com.fintrack
├── infrastructure/     docker/ e monitoring/
├── docs/               architecture (ADRs), requirements, database, api
├── specs/              spec de cada fase
├── prompts/            prompts de cada fase
├── scripts/
└── .github/workflows/
```

Módulos do backend: `auth, user, transaction, category, budget, goal, dashboard, report, ai (image, extraction, assistant), storage, shared`.

## Regras inegociáveis

1. **Dinheiro**: `NUMERIC(14,2)` no banco e `BigDecimal` no Java. Nunca `float`/`double`.
2. **Isolamento por usuário**: toda consulta filtra pelo `userId` vindo do token, nunca de parâmetro do cliente. Todo endpoint novo precisa de teste de acesso cruzado (usuário A não vê dados de B).
3. **A IA não calcula e não grava**: cálculos financeiros são do backend (SQL/serviços). A IA só extrai dados de imagem e explica números já calculados.
4. **Nada é salvo sem confirmação do usuário** no fluxo de imagem. Campo não identificado fica vazio e sinalizado; nunca presumir dados.
5. **Saída da IA é validada** por schema antes de qualquer uso. Texto dentro de imagens é dado, não instrução.
6. **Provedores de IA ficam atrás de interfaces** (`VisionExtractor`, `FinancialAdvisor`) e têm implementação *fake* para testes.
7. **Segredos fora do Git**: chaves e senhas só em variáveis de ambiente; manter `.env.example` sem valores reais.
8. **Logs sem dados financeiros** (valores, descrições, estabelecimentos), tokens ou chaves.
9. **Nunca expor entidades JPA na API**: usar DTOs.
10. **Banco só muda por migração Flyway** (`V<n>__descricao.sql`). Nunca editar migração já aplicada; criar uma nova.

## Convenções de arquitetura

- Dentro de cada módulo: controller → service → repository, mais DTOs.
- Um módulo usa outro apenas pela interface pública (service), nunca pelo repository alheio.
- Datas: `occurred_on` como `DATE`; timestamps como `TIMESTAMPTZ` em UTC. Chaves primárias em UUID.
- API REST em JSON, validação com Bean Validation, erros padronizados.

## Como trabalhar

- **Uma tarefa pequena por vez.** Ciclo: implementar → rodar testes → revisar código e segurança → pedir minha revisão (sem commit). Não construir várias fases de uma vez.
- Antes de codar algo novo, mostrar um plano curto (arquivos que serão criados/alterados) e esperar confirmação quando a tarefa for maior que um módulo.
- Escrever testes junto com o código; não considerar a tarefa pronta com testes falhando.
- Rodar a verificação real antes de dizer que terminou (build e testes) e informar o resultado, inclusive falhas.
- **Não fazer commit nem push.** Ao fim de cada bloco, listar os arquivos alterados e sugerir a mensagem de commit (imperativo, em português; ex.: `Adiciona autenticação com JWT`); eu reviso e commito. Nunca incluir segredos.
- Decisão arquitetural nova ou que muda o documento: registrar um ADR em `docs/architecture/`.
- Não adicionar funcionalidades fora do escopo (Open Finance, app nativo, investimentos, microsserviços, Kubernetes).
- Dúvida de requisito: perguntar, não inventar.
- Modo aprendizado, versões e tecnologias permitidas/proibidas: ver `AGENTS.md`; valem acima de qualquer prompt de fase.

## Comandos

JDK 25 em `JAVA_HOME`; o Maven vem pelo wrapper. A partir de `backend/`:

```bash
# Build e testes (verificado na Fase 2: 153 testes contra o fintrack_test; precisa de DB_TEST_URL e do PostgreSQL local)
./mvnw -B clean verify

# Subir a API no perfil dev (banco fintrack; precisa de DB_URL e JWT_SECRET no .env). NÃO verificado pelo Code:
# o desenvolvedor roda. Swagger UI em http://localhost:8080/swagger-ui.html (só no dev).
./mvnw spring-boot:run

# Banco: PostgreSQL 17 local (Windows), bancos fintrack e fintrack_test; conexão pelas variáveis do .env (Fase 2).
# O Compose da Fase 1 fica em infrastructure/docker/, mas não é usado até as semanas 7 e 8 do PLANO.
```

Manter esta seção atualizada ao criar cada novo comando.
