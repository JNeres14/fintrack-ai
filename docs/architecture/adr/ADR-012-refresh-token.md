# ADR-012: Refresh token opaco com rotação e detecção de reuso

Status: proposta (Fase 2, Bloco B, 09/11/2026), aprovada no chat; aguardando commit

## Decisão
- Valor: 256 bits aleatórios (`SecureRandom`), opaco (não é JWT), válido por 7 dias.
- No banco (`refresh_tokens`, migração V2) só o hash SHA-256; o valor original nunca é gravado. SHA-256 basta porque o token é longo e aleatório (BCrypt é para senhas fracas).
- Rotação: cada uso marca o token como usado (atualização atômica, para pedidos simultâneos não gerarem dois tokens) e emite um novo na mesma família (`family_id`).
- Reapresentar um token já usado: revoga toda a família e responde 401; o usuário faz login de novo. Só o userId vai para o log.
- Logout: revoga a família do refresh enviado; idempotente (token desconhecido também responde 204). O access token vale até expirar (máx. 15 min).

## Pendência
Limpeza periódica de tokens expirados (job) não está no Bloco B.
