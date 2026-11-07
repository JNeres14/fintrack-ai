## 2. Requisitos funcionais

São 28 requisitos em seis módulos. "MVP" = obrigatório na primeira versão utilizável; "P2" = fase posterior.

| ID | Módulo | Requisito | Prioridade |
| --- | --- | --- | --- |
| RF-01 | Autenticação | Cadastro, login, logout e renovação de sessão (JWT + refresh token) | MVP |
| RF-02 | Financeiro | CRUD de transações (receita/despesa) com data, valor, descrição, estabelecimento, categoria e forma de pagamento | MVP |
| RF-03 | Financeiro | Categorias e subcategorias, com um conjunto padrão e personalização pelo usuário | MVP |
| RF-04 | Financeiro | Contas financeiras (ex.: carteira, conta corrente) com saldo calculado | MVP |
| RF-05 | Financeiro | Histórico com filtros por período, categoria, tipo, conta e texto | MVP |
| RF-06 | Financeiro | Despesas recorrentes (mensal/semanal) com geração automática de lançamentos | P2 |
| RF-07 | Captura IA | Upload de imagem (JPEG, PNG, WebP; PDF opcional) com validação de tipo e tamanho | MVP |
| RF-08 | Captura IA | Extração estruturada: tipo, valor, data, estabelecimento, categoria, pagamento, descrição, confiança | MVP |
| RF-09 | Captura IA | Suporte a imagens com várias transações (extratos): uma lista de itens, cada um confirmável separadamente | MVP |
| RF-10 | Captura IA | Tela de revisão: editar qualquer campo extraído antes de salvar | MVP |
| RF-11 | Captura IA | Nunca salvar sem confirmação explícita do usuário | MVP |
| RF-12 | Captura IA | Campos não identificados ficam vazios e sinalizados; nada é presumido | MVP |
| RF-13 | Captura IA | Detecção de duplicidade: hash da imagem e transação similar (valor + data + estabelecimento) | MVP |
| RF-14 | Captura IA | Comprovante original vinculado à transação confirmada | MVP |
| RF-15 | Assistente | Chat que responde perguntas sobre os gastos usando dados consultados pelo backend | MVP |
| RF-16 | Assistente | Resumo mensal gerado pela IA a partir de números calculados pelo backend | MVP |
| RF-17 | Assistente | Análises proativas: aumento de gastos, recorrências, padrões, sugestões de economia, projeção até o fim do mês | P2 |
| RF-18 | Dashboard | Saldo, receitas, despesas, gastos do mês, comparação com o mês anterior | MVP |
| RF-19 | Dashboard | Distribuição por categoria e últimas movimentações | MVP |
| RF-20 | Orçamentos | Limite mensal geral e por categoria, com progresso | MVP |
| RF-21 | Orçamentos | Alertas ao se aproximar do limite (ex.: 80% e 100%) | P2 |
| RF-22 | Metas | Metas de economia com valor-alvo, prazo e progresso | P2 |
| RF-23 | Relatórios | Relatório mensal e anual | P2 |
| RF-24 | Relatórios | Evolução de receitas/despesas e comparação entre categorias | P2 |
| RF-25 | Relatórios | Exportação em CSV e PDF | P2 |
| RF-26 | PWA | Instalável no celular, layout responsivo, upload pela câmera/galeria | MVP |
| RF-27 | Plataforma | Todos os dados isolados por usuário | MVP |
| RF-28 | Plataforma | Exclusão de conta e dos dados/imagens do usuário | P2 |

Regra transversal: a IA nunca cria, altera ou exclui dados por conta própria; todo lançamento passa por ação explícita do usuário.

