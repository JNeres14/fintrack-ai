# FinTrack AI — Documento Técnico Inicial

Versão de 06/11/2026. Os diagramas abaixo estão em Mermaid (renderizam no GitHub e em editores Markdown com suporte).

## 1. Visão geral e escopo

O FinTrack AI é um gerenciador financeiro pessoal (web responsiva + PWA) em que o usuário envia imagens de comprovantes e a IA extrai as transações para revisão e confirmação. Este documento é a referência única para o Claude Code implementar o projeto em tarefas pequenas e verificáveis.

**Objetivos**

1. Eliminar o cadastro manual de cada despesa.
2. Centralizar receitas, despesas e movimentações.
3. Dar uma visão clara da situação financeira (dashboard).
4. Usar IA para identificar padrões e oportunidades de economia.
5. Sincronizar dados entre celular e computador.

**Decisões de produto**

| Decisão | Definição |
| --- | --- |
| Público | Uso pessoal, com arquitetura pronta para multiusuário |
| Plataformas | Web responsiva + PWA |
| Registro principal | Imagem analisada por IA, sempre com confirmação |
| Registro alternativo | Manual (correções e gastos sem comprovante) |
| IA | Visão (extração) + consultoria (assistente) |
| Cálculos financeiros | Sempre no backend; a IA apenas interpreta os resultados |
| Backend | Java + Spring Boot, monólito modular |
| Banco | PostgreSQL |
| Infraestrutura | Docker + CI/CD (GitHub Actions) |

**Fora do escopo inicial:** Open Finance, sincronização de cartões, app nativo Android/iOS, investimentos, previsões avançadas, notas fiscais complexas, microsserviços e Kubernetes.

O MVP está concluído quando for possível: acessar por celular e PC, logar, enviar um print, corrigir e confirmar os dados extraídos, ver a transação no dashboard, consultar gastos por período, conversar com o assistente, rodar tudo com Docker, publicar por CI/CD e monitorar em produção.

