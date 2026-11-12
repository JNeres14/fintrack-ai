# ADR-011: Política de senha e hash BCrypt

Status: aceita (proposta em 09/11/2026; aceita no fechamento da Fase 2, 11/11/2026)

## Decisão
- Senha: mínimo 12 caracteres; máximo 72 bytes (o BCrypt ignora o que passa disso, então recusamos em vez de truncar); sem exigir símbolos; recusa igual ao e-mail e uma lista pequena de senhas comuns. Comprimento vale mais que complexidade.
- Hash: BCrypt, custo 12 (`BCryptPasswordEncoder`). Custo 4 só no perfil `test`, para os testes ficarem rápidos.
- No login, e-mail inexistente também faz uma comparação BCrypt com um hash falso, para o tempo de resposta não revelar se o e-mail existe.

## Alternativas descartadas
Argon2: melhor, mas exige Bouncy Castle (dependência fora da lista permitida). O hash guarda o algoritmo, então migrar depois é possível sem quebrar usuários.
