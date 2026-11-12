# SPEC — Decisões da rodada de 11/11/2026 (Fases 3, 4 e 6)

> Parte do `SPEC.md`. **Não altera a Fase 2**: `specs/SPEC-fase-2.md` e o código seguem como estão.
> Onde divergir de `specs/SPEC-decisoes-fase-4.md` ou de `specs/SPEC-cartao-e-cashback.md`, vale este arquivo.
> Status: decisões minhas, tomadas em conversa em 11/11/2026; aguardando revisão deste arquivo.

## 1. Extração por print (Fase 4)
- **Hora da compra:** guardada só na extração, para detectar duplicidade. A transação continua só com a data (sem migração).
- **Data de referência:** ao enviar o print, o app sugere hoje e eu posso corrigir. "Quinta" vira a última quinta antes dessa data. Motivo: o app do banco mostra só o dia da semana nas compras recentes, sem mês nem ano.
- **Frequência:** semanal (7 dias ou menos), para o dia da semana ficar sempre sem ambiguidade.
- **Duplicidade:** compara estabelecimento, valor, data e hora. Duas compras iguais em horas diferentes são distintas. Prints semanais que se sobrepõem não podem duplicar transações.
- **Imagem:** apagada **ao confirmar** o rascunho. Rascunho não confirmado continua expirando em 30 dias. Isso substitui a regra da seção 2.4 de `SPEC-decisoes-fase-4.md` ("a imagem fica enquanto a transação existir"). Fica resolvida a pendência do prazo de retenção.

## 2. Lembrete semanal para enviar o print
- **Canal:** bot do Telegram. A mensagem é genérica ("hora de enviar o print"), sem valores nem estabelecimentos.
- **Como dispara:** agendador (cron semanal) do GitHub Actions, que roda na nuvem e **não depende do notebook ligado**. Token do bot e identificador do chat ficam nos segredos do repositório, nunca no código.
- **Quando fazer:** semanas 9 e 10 do `PLANO.md`, junto com o estudo de GitHub Actions, em modo tutor (o autor escreve; o Claude dá dicas e revisa). Até lá, o lembrete recorrente do iPhone é opcional.
- **Cuidados:** o horário pode atrasar alguns minutos; o GitHub desativa agendamentos de repositório parado por 60 dias. Mudar a frequência é mudar o cron.

## 3. Fase 3
- **Parcelas (D3):** entram no **início da Fase 3**, junto com a aba do cartão. A decisão é não fazer na Fase 2; o item "D3 opcional" do `SPEC-fase-2.md` fica como está, mas não será executado nela.
- **Cashback:** início da Fase 3 (`SPEC-cartao-e-cashback.md`, seção 4).
- **Ajustes de API do cartão:** início da Fase 3 (`SPEC-cartao-e-cashback.md`, seção 6).
- **Dashboard, primeira tela:** saldo e totais do mês, com comparação com o mês anterior. Gastos por categoria, orçamentos e cartão com cashback ficam fora da primeira tela, em telas ou abas próprias (a aba de cartão continua prevista).
- **HTTPS no celular:** HTTPS do Tailscale (grátis, sem instalar certificado, e já serve para uso fora de casa na Fase 6). Registrar em ADR na Fase 3. Enquanto isso, o envio pela galeria funciona sem HTTPS.
- **Senha esquecida:** comando local de redefinição, pelo terminal, feito por mim como dono do banco. Sem fluxo por e-mail e sem tela. É uma tarefa pequena depois de fechar a Fase 2.

## 4. Hospedagem (Fase 6)
- **Decisão (11/11/2026):** o backend e o banco ficam no **PC do desenvolvedor, com acesso por Tailscale**. Foram pesquisados os planos gratuitos e nenhum atende (Spring Boot sempre ligado + PostgreSQL + grátis + confiável + dados financeiros): Render dorme após 15 min e o Postgres grátis expira em 30 dias; Fly.io não tem plano grátis para contas novas; Railway tem só US$ 1/mês de crédito; AWS novo plano grátis fecha a conta em até 6 meses; Oracle Always Free reduziu o ARM para 2 OCPU/12 GB em 15/06/2026, sem aviso público e com regras de cobrança ambíguas; Neon/Supabase servem só para o banco e pausam por inatividade.
- **Conflito resolvido:** extração local (OCR/modelo de visão) exige o PC de qualquer forma, o que reforça a decisão.
- **Reavaliar:** na Fase 6, só se surgir necessidade (ex.: PC desligado com frequência). Nesse caso, estudar Oracle Always Free como laboratório de AWS/Linux, com backup semanal (pg_dump). Registrar em ADR na Fase 6.

## 5. Escopo e prazo
- Sem prazo fixo para o FinTrack. Nada foi cortado por ora. A lista de candidatos a corte (cashback, parcelas, lembrete, detecção de duplicidade) fica para reavaliar ao fim de cada fase.

## 6. Continua em aberto
- API de IA opcional (só se o OCR ou o modelo local ficar abaixo do limiar de 85%).
- Medição de acerto com 20 a 30 comprovantes reais, no início da Fase 4.
- PDF: fora do MVP.
