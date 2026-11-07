# Prompt — Fase 1 (Planejamento e estruturação)

Cole o texto abaixo no Claude Code, na pasta onde o repositório `fintrack-ai` será criado. Antes, coloque o documento técnico exportado em Markdown e o `CLAUDE.md` nessa pasta (o prompt os procura em `docs/` e na raiz).

---

Vamos começar a Fase 1 do FinTrack AI (planejamento e estruturação). Ainda não vamos implementar nenhuma regra de negócio.

Leia primeiro o `CLAUDE.md` na raiz e o documento técnico em `docs/`. Eles são a referência do projeto.

**Objetivo da Fase 1:** deixar o repositório estruturado, documentado e pronto para a Fase 2, com tudo versionado em Git.

**Tarefas (faça nesta ordem e me mostre o plano antes de começar):**

1. Inicializar o repositório Git com a estrutura de pastas do `CLAUDE.md` (`frontend/`, `backend/`, `infrastructure/docker/`, `infrastructure/monitoring/`, `docs/architecture/`, `docs/requirements/`, `docs/database/`, `docs/api/`, `scripts/`, `.github/workflows/`). Pastas vazias recebem um `.gitkeep`.
2. Criar `.gitignore` adequado (Java/Maven, Node, IDEs, `.env`, arquivos de build) e `.env.example` com os nomes das variáveis previstas, sem valores reais.
3. Organizar `docs/`:
   - Dividir o documento técnico em arquivos menores por assunto, quando fizer sentido: requisitos em `docs/requirements/`, modelo de dados em `docs/database/`, arquitetura em `docs/architecture/`. Preservar o conteúdo original, sem reescrever decisões.
   - Criar os ADRs 001 a 007 em `docs/architecture/adr/` a partir da tabela de decisões do documento, no formato: contexto, decisão, consequências, alternativa descartada.
4. Escrever um `README.md` inicial: o que é o projeto, stack, como a documentação está organizada, status (Fase 1) e uma seção "Como rodar" marcada como "a definir na Fase 2".
5. Criar `docs/ROADMAP.md` com as 7 fases, o escopo de cada uma e o critério de aceite proposto, copiados do documento, com checkboxes.
6. Criar o esqueleto do backend sem lógica: projeto Spring Boot (Java 21, Maven) em `backend/` com pacote base `com.fintrack`, os pacotes vazios dos módulos listados no `CLAUDE.md` (com `package-info.java` descrevendo a responsabilidade de cada um), e um único teste de contexto que passa. Dependências mínimas por enquanto: Web, Validation, Security, Data JPA, PostgreSQL driver, Flyway, Actuator, Lombok opcional (pergunte antes de usar Lombok).
7. Criar `infrastructure/docker/docker-compose.yml` apenas com o PostgreSQL para desenvolvimento (volume nomeado, variáveis vindas do `.env`, healthcheck). Ainda não containerizar o backend.
8. Criar um workflow mínimo em `.github/workflows/backend-ci.yml` que compila e roda os testes do backend em cada push e pull request.
9. Registrar em `docs/prototypes/` uma lista das telas do MVP (login, dashboard, histórico de transações, lançamento manual, upload e revisão por IA, chat do assistente, orçamentos) com uma descrição curta do conteúdo de cada uma. Sem desenhar interface ainda.

**Regras para esta fase:**

- Não implementar entidades, endpoints, autenticação nem frontend.
- Não inventar requisitos: o que não estiver no documento, pergunte.
- Se algo no documento estiver ambíguo ou conflitante, liste as dúvidas no final em vez de decidir sozinho.
- Fazer commits pequenos, um por tarefa, com mensagens em português no imperativo. Não fazer push.

**Verificação antes de encerrar:**

- Rodar o build e o teste de contexto do backend e me mostrar o resultado real.
- Subir o PostgreSQL com `docker compose` e confirmar que o healthcheck fica saudável (se Docker não estiver disponível no ambiente, diga isso em vez de supor que funcionou).
- Listar a árvore final do repositório e o resumo dos commits.
- Atualizar a seção "Comandos" do `CLAUDE.md` com os comandos de build, teste e subida do banco que acabaram de funcionar.

Ao terminar, me diga o que foi feito, o que não pôde ser verificado e as dúvidas em aberto, e pare. Não comece a Fase 2.

---

## Antes de rodar

- Exporte o documento técnico do Claude como Markdown e salve em `docs/` (por exemplo `docs/documento-tecnico.md`).
- Java 21 e Maven são suposições minhas; se você usa outra versão ou prefere Gradle, troque no item 6.
- Confirme se Docker está instalado na sua máquina (Windows 11: Docker Desktop com WSL2) para o item 7.
