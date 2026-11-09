# ADR-010: JWT com o suporte do Spring Security (Nimbus), HS256 e segredo só em variável de ambiente

Status: proposta (Fase 2, Bloco B, 09/11/2026), aprovada no chat; aguardando commit

## Contexto
A API precisa de token de acesso curto (RNF-02, máx. 15 min). Spring Boot 4.1.2 e Java 25.

## Decisão
- Biblioteca: `spring-boot-starter-security-oauth2-resource-server` (Spring Security 7.1.2 + Nimbus JOSE 10.9.1), já no BOM do Boot. Emitimos com `JwtEncoder` e validamos com `JwtDecoder` e o filtro pronto do Spring; não escrevemos filtro de validação.
- Algoritmo HS256 fixo (o decoder recusa qualquer outro, inclusive `none`). Valida assinatura, emissor (`fintrack-ai`) e expiração, com tolerância de relógio zero (o padrão de 60 s deixaria token vencido valer mais um minuto).
- Token de acesso: 15 min (a configuração recusa mais que 15). Claims: `sub` = userId, `iss`, `iat`, `exp`, `jti`. Nada de e-mail ou dado financeiro.
- `JWT_SECRET` só em variável de ambiente, sem valor padrão. Ausente, vazio ou com menos de 32 bytes: a aplicação não sobe. Nos testes há uma chave fixa só em `src/test`.
- O `userId` autenticado vem de um único componente (`shared.CurrentUser`), lendo o `sub` do token validado.

## Consequências
Revogar um access token antes do prazo não é possível (até 15 min de janela); aceito. Trocar o segredo invalida todos os tokens de acesso. O nome "oauth2" no artefato não implica uso de OAuth2.

## Alternativas descartadas
jjwt e java-jwt (fora do BOM, exigiriam filtro próprio); Nimbus puro (sem o filtro do Spring).
