# ADR-013: Bloqueio por tentativas falhas e rate limiting em memória

Status: proposta (Fase 2, Bloco B, 09/11/2026), aprovada no chat; aguardando commit

## Decisão
- Bloqueio: 5 falhas seguidas por e-mail bloqueiam por 15 min (`users.failed_attempts`, `users.locked_until`). Login certo zera o contador. A resposta é o mesmo 401 genérico, sem dizer "bloqueada". Configurável por variável de ambiente.
- Rate limiting (RNF-13): janela fixa de 1 min, em memória, sem biblioteca, por endpoint + IP (`getRemoteAddr`; `X-Forwarded-For` é ignorado de propósito, pois o cliente poderia forjá-lo). Padrão 10 por minuto em login, cadastro, refresh e logout; resposta 429 com `Retry-After`. É checado antes de tocar no banco e no BCrypt.

## Consequências
O rate limit não é compartilhado entre instâncias e zera ao reiniciar (aceito na spec para uso pessoal). Um atacante pode bloquear a conta de um e-mail conhecido por 15 min (troca conhecida; o rate limit por IP limita o dano). O contador não é atômico sob concorrência forte; aceitável aqui. Atrás de proxy (Fase 6), revisar a origem do IP.
