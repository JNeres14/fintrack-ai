# Telas do MVP

Lista das telas, sem desenho de interface. O conteúdo de cada tela deriva dos requisitos e casos de uso em `docs/requirements/`; o que é proposta (e não está no documento) está marcado com "(proposta)". Todas as telas devem funcionar de 360 px a desktop (RNF-18).

## 1. Login e cadastro (RF-01, UC-01)
- Formulário de e-mail e senha; opção de criar conta.
- Mensagens de erro genéricas para credenciais inválidas; aviso claro de bloqueio temporário ou limite de tentativas (RNF-13).
- A sessão é renovada em segundo plano (refresh token); logout encerra a sessão.

## 2. Dashboard (RF-18, RF-19, UC-05)
- Saldo, receitas, despesas e gastos do mês, com comparação com o mês anterior.
- Distribuição de gastos por categoria.
- Últimas movimentações.
- Resumo mensal gerado pela IA (RF-16), a partir de números calculados pelo backend. Entra na Fase 5.

## 3. Histórico de transações (RF-05, UC-04)
- Lista paginada, da data mais recente para a mais antiga.
- Filtros por período, categoria, tipo, conta e texto (descrição ou estabelecimento).
- Acesso a editar ou excluir uma transação (proposta).

## 4. Lançamento manual (RF-02, UC-02)
- Formulário com tipo (receita ou despesa), valor, data, estabelecimento, descrição, categoria, forma de pagamento e conta.
- Valor positivo com duas casas decimais; o sinal vem do tipo.
- Funciona mesmo se a IA estiver indisponível (RNF-14).

## 5. Upload e revisão por IA (RF-07 a RF-14, UC-03)
- Envio de imagem pela câmera ou galeria (JPEG, PNG, WebP; limite de tamanho), com indicador de progresso durante a extração (RNF-09).
- Aviso de imagem já enviada (hash) e de imagem ilegível ou sem transação, com opção de lançamento manual.
- Lista de transações extraídas (uma ou várias, no caso de extratos), cada uma editável em todos os campos.
- Campos não identificados vazios e sinalizados; possíveis duplicatas marcadas; indicador de confiança.
- Confirmar ou descartar cada item, ou em lote. Nada é salvo antes da confirmação (RF-11).
- Entra na Fase 4.

## 6. Chat do assistente (RF-15, UC-06)
- Conversa em texto sobre os gastos; as respostas usam números calculados pelo backend.
- Mensagem clara se a IA estiver indisponível ou o limite de uso for atingido.
- Entra na Fase 5.

## 7. Orçamentos (RF-20, UC-07)
- Definição de limite mensal geral e por categoria.
- Progresso do consumo atual de cada limite.
- Alertas de 80% e 100% (RF-21) são P2.
