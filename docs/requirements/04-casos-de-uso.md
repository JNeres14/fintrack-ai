## 4. Casos de uso

O ator principal é o **Usuário** autenticado; a **IA** e o **Armazenamento** são sistemas de apoio acionados pelo backend.

| ID | Caso de uso | Resumo |
| --- | --- | --- |
| UC-01 | Cadastrar e autenticar | Cria conta, faz login, renova e encerra a sessão |
| UC-02 | Lançar transação manualmente | Preenche o formulário e salva; o dashboard é atualizado |
| UC-03 | **Registrar transação por imagem** | Fluxo principal, detalhado abaixo |
| UC-04 | Consultar histórico | Filtra por período, categoria, tipo e conta |
| UC-05 | Ver dashboard | Saldo, totais, comparativo mensal, categorias, resumo da IA |
| UC-06 | Conversar com o assistente | Faz perguntas e recebe respostas baseadas em dados reais |
| UC-07 | Definir orçamento | Limite mensal geral ou por categoria; acompanha o consumo |
| UC-08 | Criar meta de economia | Valor-alvo, prazo e progresso |
| UC-09 | Gerar e exportar relatório | Mensal ou anual, em CSV ou PDF |

**UC-03 — Registrar transação por imagem**

1. O usuário envia uma imagem (print de Pix, extrato, comprovante ou compra).
2. O backend valida tipo, tamanho e conteúdo, calcula o hash e verifica se a mesma imagem já foi enviada.
3. A imagem é guardada em armazenamento privado e enviada à API de IA multimodal.
4. A IA devolve JSON estruturado (uma ou mais transações); o backend valida campos, tipos e valores.
5. O backend marca possíveis duplicatas e campos não identificados.
6. O usuário revisa, edita e confirma cada transação (ou descarta).
7. Somente após a confirmação, a transação é gravada e vinculada ao comprovante; o dashboard é atualizado.

*Fluxos alternativos:* imagem ilegível ou sem transação → aviso e opção de lançamento manual; duplicata detectada → usuário decide manter ou descartar; falha da API de IA → mensagem clara e cadastro manual disponível; extrato com várias linhas → confirmação individual ou em lote.

**UC-06 — Conversar com o assistente:** o backend identifica a intenção e o período da pergunta, calcula os números necessários e envia apenas esse resultado à IA, que explica tendências e sugere ações sem recalcular valores.

