## 8. Segurança e privacidade

Segurança entra desde a Fase 2; o checklist abaixo vira critério de aceite das fases.

| Área | Controle | Fase |
| --- | --- | --- |
| Autenticação | Hash BCrypt/Argon2; JWT curto + refresh com rotação; bloqueio após tentativas falhas | 2 |
| Autorização | Todo repositório filtra por `user_id`; teste de acesso cruzado entre usuários | 2 |
| Validação | Bean Validation em DTOs; nunca expor entidades JPA diretamente | 2 |
| Uploads | Tipo verificado pelo conteúdo, limite de tamanho, nome gerado (UUID), sem execução | 4 |
| Armazenamento | Bucket/diretório privado; URLs assinadas e temporárias | 4 |
| Abuso | Rate limiting em login, upload e chat; limite diário de chamadas à IA | 4 |
| Transporte | HTTPS obrigatório, HSTS, CORS restrito ao domínio do frontend | 6 |
| Segredos | Variáveis de ambiente / secret manager; `.gitignore` e varredura de segredos no CI | 2 |
| Logs | Sem valores, descrições, tokens ou chaves; IDs de correlação | 6 |
| IA e privacidade | Dados mínimos enviados ao provedor; política de retenção conferida; prompts tratam texto da imagem como dado, não instrução | 4–5 |
| Dependências | Dependabot e verificação de vulnerabilidades no pipeline | 6 |
| Backup | Backup diário criptografado do banco com teste de restauração | 6 |

Risco específico de IA: uma imagem pode conter texto que tenta instruir o modelo (*prompt injection*). Mitigação: saída sempre validada por schema, IA sem permissão de escrita e confirmação humana antes de salvar.

