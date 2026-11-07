## 3. Requisitos não funcionais

Os alvos abaixo são metas iniciais para um projeto pessoal; devem ser revisados após medição real.

| ID | Categoria | Requisito | Meta / verificação |
| --- | --- | --- | --- |
| RNF-01 | Segurança | Senhas com hash seguro (BCrypt ou Argon2) | Nenhuma senha em texto puro no banco ou logs |
| RNF-02 | Segurança | JWT de curta duração + refresh token com rotação | Access token ≤ 15 min |
| RNF-03 | Segurança | Autorização por usuário em toda consulta | Teste automatizado: usuário A não acessa dados de B |
| RNF-04 | Segurança | Segredos e chaves de API fora do repositório | Variáveis de ambiente; varredura de segredos no CI |
| RNF-05 | Privacidade | Imagens em armazenamento privado, acesso por URL temporária | URL expira em minutos |
| RNF-06 | Privacidade | Logs sem dados financeiros sensíveis | Revisão de logs; mascaramento de valores e textos |
| RNF-07 | Privacidade | O assistente recebe só os dados necessários à pergunta | Nunca o histórico completo |
| RNF-08 | Desempenho | Respostas da API (exceto IA) rápidas | p95 < 500 ms em consultas comuns |
| RNF-09 | Desempenho | Extração de uma imagem | Resposta em até ~15 s, com indicador de progresso |
| RNF-10 | Integridade | Valores monetários em NUMERIC(14,2), nunca float/double | Revisão de código + teste de arredondamento |
| RNF-11 | Integridade | Migrações versionadas (Flyway) | Banco reproduzível do zero |
| RNF-12 | Uploads | Limite de tamanho e tipo; validação pelo conteúdo, não só pela extensão | Ex.: máx. 10 MB; JPEG/PNG/WebP |
| RNF-13 | Resiliência | Rate limiting nos endpoints de login, upload e chat | Respostas 429 testadas |
| RNF-14 | Resiliência | Falha da API de IA não impede o cadastro manual | Degradação graciosa com mensagem clara |
| RNF-15 | Qualidade | Cobertura de testes nas regras de negócio e segurança | JUnit/Mockito/Testcontainers; Vitest no frontend |
| RNF-16 | Operação | Ambiente reproduzível e deploy automatizado | `docker compose up` e pipeline CI/CD |
| RNF-17 | Operação | Métricas e logs estruturados; backup diário do banco | Prometheus/Grafana; restauração testada |
| RNF-18 | Usabilidade | Interface responsiva, uso confortável no celular | Testes em telas de 360 px a desktop |
| RNF-19 | Manutenibilidade | Decisões arquiteturais registradas | ADRs em `docs/architecture` |

