# ADR-009: Testes de banco no PostgreSQL local, com trava

Status: proposta (Fase 2, Bloco A, 07/11/2026), aguardando aprovação

## Contexto
A Fase 2 não usa Docker nem Testcontainers (semanas 7 a 10 do PLANO). Os testes precisam de um PostgreSQL real, e a Fase 1 os rodava sem banco (autoconfiguração excluída). Testes que limpam o schema, apontados por engano para o banco `fintrack`, apagariam os dados de desenvolvimento.

## Decisão
- Os testes rodam no perfil `test`, ativado pelo Surefire, e leem só `DB_TEST_URL` (banco `fintrack_test`), nunca `DB_URL`.
- Ao subir o contexto, uma `FlywayMigrationStrategy` existente só em `src/test` confere `current_database() = 'fintrack_test'` e só então faz `clean` + `migrate`. Qualquer outro nome aborta antes de apagar. O mesmo vale para o truncate usado em testes de fluxo completo.
- `spring.flyway.clean-disabled=true` em todos os perfis, exceto `test`.
- Testes de repository e de schema usam `@Transactional` com rollback; o contexto é reaproveitado entre classes, então as migrações rodam uma vez por execução.
- O `.env` da raiz é lido por `spring.config.import=optional:file:../.env[.properties]`; variáveis de ambiente do sistema têm prioridade.

## Consequências
`./mvnw verify` exige o PostgreSQL local rodando e `DB_TEST_URL` configurada; sem isso falha (o que é desejado). Não roda em CI até o Docker entrar (semanas 7 a 10), quando Testcontainers substitui este esquema. A trava protege contra o erro de configuração mais provável, mas não contra um `DB_TEST_URL` que aponte para outro banco de teste com o mesmo nome em outro servidor.

## Alternativas descartadas
H2 (comportamento diferente do PostgreSQL: `CHECK`, índices parciais e de expressão); exclusão de autoconfiguração (não valida o banco); `clean` sem trava.
