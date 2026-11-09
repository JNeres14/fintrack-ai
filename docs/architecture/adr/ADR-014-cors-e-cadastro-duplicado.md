# ADR-014: CORS restrito por configuração e cadastro com e-mail repetido

Status: proposta (Fase 2, Bloco B, 09/11/2026), aprovada no chat; aguardando commit

## Decisão
- CORS: origens em `CORS_ALLOWED_ORIGINS` (lista separada por vírgula). Vazio = nenhuma origem; curinga `*` faz a aplicação não subir. Sem cookies (`allowCredentials=false`): o token vai no cabeçalho `Authorization`, e CSRF fica desligado, com sessão STATELESS.
- Cadastro com e-mail já existente: 409 com "Já existe um cadastro com este e-mail. Esqueceu a senha?". Escolhido pela clareza para quem usa o app, mesmo revelando que o e-mail existe (enumeração de contas). Mitigação: rate limit no cadastro.

## Pendências e revisão
O fluxo de "esqueci a senha" ainda não existe (exige envio de e-mail); a mensagem já o antecipa. Se o app for aberto ao público, reavaliar a mensagem do 409 (resposta genérica + e-mail de aviso).
