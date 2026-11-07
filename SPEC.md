# SPEC — FinTrack AI

> Fonte de verdade do projeto. Atualizar sempre que uma decisão mudar.
> Contexto de carreira e modo de trabalho: ../../PLANO.md.
> Versões e tecnologias permitidas/proibidas ficam no AGENTS.md (não repetir aqui).
> Spec de cada fase: `specs/`. Requisitos detalhados (RF, RNF, casos de uso, modelo de dados, segurança): `docs/`. Este arquivo não os duplica; só resume, decide e acompanha as fases.

## 1. Objetivo
Gerenciador financeiro pessoal (web + PWA) em que o usuário envia imagens de comprovantes (print de Pix, extrato, compra), a IA extrai as transações, o usuário revisa e confirma, e um assistente analisa os gastos a partir de números calculados pelo backend. Uso pessoal, com arquitetura pronta para múltiplos usuários.

## 2. Por que este projeto (ligação com o PLANO.md)
É o projeto central do portfólio. Treina backend Java/Spring Boot (alvo de estágio), segurança de aplicação, IA aplicada e, em camadas, a trilha de infraestrutura: Docker → Compose → CI → AWS → Terraform → observabilidade → segurança. Cada camada vira uma tag/release e uma seção no README.

## 3. MVP
Fases entre parênteses.
- [ ] Acessar pelo celular e pelo computador (3)
- [ ] Fazer login com dados sincronizados (2, 3)
- [ ] Lançar transações manualmente e consultar por período, categoria, tipo e conta (2, 3)
- [ ] Ver o dashboard com saldo, totais e comparação mensal (3)
- [ ] Enviar um print, receber os dados extraídos pela IA, corrigir e confirmar (4)
- [ ] Ver a transação confirmada no dashboard (4)
- [ ] Definir orçamento e acompanhar o consumo (2, 5)
- [ ] Conversar com o assistente e receber análises baseadas em dados reais (5)
- [ ] Rodar tudo com Docker, publicar por CI/CD e monitorar (6)

## 4. Fora do escopo
Open Finance, sincronização de cartões, app nativo Android/iOS, investimentos, previsões avançadas, notas fiscais complexas, microsserviços, Kubernetes. AWS e Terraform só a partir da Fase 6.

## 5. Decisões técnicas
Só o porquê. Detalhes em `docs/architecture/adr/`.
| Decisão | Escolha | Por quê | Alternativa descartada |
|---|---|---|---|
| Arquitetura | Monólito modular (ADR-001) | Menos complexidade operacional; evolui por módulos | Microsserviços |
| Plataforma | Web + PWA (ADR-002) | Um código para celular e PC | App nativo |
| Dinheiro | `NUMERIC(14,2)` / `BigDecimal` (ADR-003) | Precisão financeira | `float`/`double` |
| Cálculos | No backend; a IA só interpreta (ADR-004) | Resultados verificáveis e testáveis | IA calculando |
| Confirmação | Humana antes de salvar (ADR-005) | A IA pode errar | Salvamento automático |
| Provedor de IA | Atrás de interface (ADR-006) | Troca e testes com *fakes* | Acoplar a um SDK |
| Imagens | Armazenamento privado, URL temporária (ADR-007) | Privacidade | Arquivos públicos ou no banco |
| Versões | Java 25 e Spring Boot 4.1.2 (ADR-008) | Só havia o JDK 25 instalado; LTS | Java 21 |
| Build | Maven Wrapper | Sem instalar Maven global | Gradle |
| DTOs | `record`, sem Lombok | Menos mágica, mais clareza para aprender | Lombok |
| Testes de banco | PostgreSQL local, banco `fintrack_test` separado (Fase 2); Testcontainers só depois do Docker | Docker entra no PLANO nas semanas 7 e 8 | H2 / exclusão de autoconfiguração |
| Schema | Só por migração Flyway | Banco reproduzível do zero | `ddl-auto` create/update |

## 6. Modo de trabalho por parte
| Parte | Modo | Observação |
|---|---|---|
| Dockerfile / Compose / CI / Terraform / scripts | Tutor (eu escrevo) | Claude dá dicas e revisa |
| Código da aplicação (Spring, React) | Acelerador | Claude gera, eu reviso e explico cada arquivo antes de commitar |
| Segurança (auth, JWT, autorização) | Acelerador com revisão reforçada | Claude explica cada decisão; eu confirmo que sei reproduzir |

Exceção registrada: `docker-compose.yml` e `backend-ci.yml` foram escritos pelo Code na Fase 1, antes desta regra. São ponto de partida; eu os revisito e reescrevo em modo tutor na Fase 6. Até lá, o Code não os altera sem pedir.
Commits: o Code não commita. No fim de cada bloco, sugere os arquivos e a mensagem; eu reviso e commito.

## 7. Fases
Cada fase termina com algo que eu consigo testar.
| Fase | Entrega | Como testar | Status |
|---|---|---|---|
| 1 | Documentação, repositório e esqueleto do backend | `./mvnw -B verify` passa; PostgreSQL do Compose fica `healthy` | [~] feita, aguarda minha aprovação do documento técnico (`specs/SPEC-fase-1.md`) |
| 2 | API financeira funcional (`specs/SPEC-fase-2.md`) | Migrações do zero; testes verdes contra o PostgreSQL local; isolamento entre usuários; Swagger no perfil dev | [ ] |
| 3 | Frontend + dashboard (inclui os endpoints de resumo no backend) | Login, lançamento manual e dashboard no celular e no PC | [ ] |
| 4 | Lançamento por imagem com IA (inclui `Attachment`, `AIAnalysis` e `attachment_id`) | Print de Pix vira transação só após confirmação; extrato com várias linhas; duplicata sinalizada | [ ] |
| 5 | Assistente financeiro, alertas de orçamento | Respostas batem com os números do backend | [ ] |
| 6 | Docker completo, CI/CD, deploy, monitoramento, backup (modo tutor) | `docker compose up` sobe tudo; merge na `main` publica; restauração testada | [ ] |
| 7 | Testes de integração e segurança, documentação da API, README, vídeo | Checklist de segurança (`docs/architecture/03-seguranca-e-privacidade.md`) cumprido | [ ] |

### Specs por fase
- Fase 1 (feita, aguarda aprovação): `specs/SPEC-fase-1.md`
- Fase 2 (próxima): `specs/SPEC-fase-2.md` (escopo, blocos, decisões, riscos e aceite)

Cada spec de fase é escrita antes de a fase começar e fica como registro quando ela termina. Este arquivo mantém só o resumo geral.

## 8. Requisitos não funcionais
Detalhados em `docs/requirements/03-requisitos-nao-funcionais.md`. Os do PLANO, sempre:
- Configs e senhas em variáveis de ambiente (`.env` no `.gitignore`)
- Endpoint `/health`
- Testes automatizados
- README explicando como rodar do zero
- Nenhum dado bancário real nem chave de API no repositório

## 9. Riscos e dúvidas em aberto
- Compatibilidade do springdoc e da biblioteca de JWT com Spring Boot 4.1.2 e Java 25: verificar nos Blocos B e E.
- O workflow de CI nunca rodou (não há remoto no GitHub) e fica parado até as semanas 9 e 10 do PLANO.
- API multimodal de IA, custo por imagem, retenção de dados do provedor: decidir antes da Fase 4.
- Hospedagem e armazenamento de imagens: decidir antes da Fase 6 (e do armazenamento na Fase 4).
- PDF no MVP ou só imagem; moeda única: decidir antes da Fase 4.
- Aprender versus gerar: se eu não conseguir explicar um arquivo gerado, a fase não está pronta.
- Calendário: o semestre ainda está em andamento; ritmo de poucas horas por semana até as férias (dez/2026).

## 10. Registro de decisões
- 2026-11-06: documento técnico inicial criado (aguarda minha aprovação; ROADMAP da Fase 1 segue desmarcado).
- 2026-11-07: Java 25 e Spring Boot 4.1.2 (ADR-008); Maven Wrapper; sem Lombok, DTOs como `record`.
- 2026-11-07: Fase 2 sem `Attachment`/`attachment_id` (entram na Fase 4); endpoints de dashboard ficam no início da Fase 3.
- 2026-11-07: o Code não commita; eu revisito e commito (alinhado ao PLANO.md e ao AGENTS.md).
- 2026-11-07: Compose e CI da Fase 1 são ponto de partida; reescrita em modo tutor na Fase 6.
- 2026-11-07: Semgrep, Trivy e Dependabot ficam fora da Fase 2 (semanas 9 e 10 do PLANO).
- 2026-11-07: spec de cada fase em `specs/` (SPEC-fase-1 como registro, SPEC-fase-2 como plano); o SPEC.md fica só com o resumo geral.
- 2026-11-07: Docker, Compose, Testcontainers e CI ficam fora da Fase 2 (semanas 7 a 10 do PLANO); a Fase 2 usa PostgreSQL 17 instalado no Windows, com os bancos `fintrack` e `fintrack_test`.
