# ADR-019: OpenAPI (springdoc) só no perfil dev

Status: aceita (aprovada pelo autor em 11/11/2026; implementada na Fase 2)

## Decisão
- springdoc-openapi-starter-webmvc-ui 3.2.0 (o pom dele usa Spring Boot 4.1.2 como base; verificado em execução pelos testes).
- Dependência no projeto, mas desligada em todo perfil; só o `dev` liga `/v3/api-docs` e `/swagger-ui`. Cadeia de segurança própria, só leitura, existe apenas no `dev` (`DevDocsConfig`). Fora do `dev`: 401 sem token e 404 com token.
- Botão Authorize com o accessToken do login.
