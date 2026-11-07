# ADR-005: Confirmação humana antes de salvar

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
A IA pode errar e a confiança que ela devolve é só uma estimativa. Imagens também podem conter texto que tenta instruir o modelo.

## Decisão
Nenhuma transação extraída por imagem é gravada sem confirmação explícita do usuário. Campos não identificados ficam vazios e sinalizados.

## Consequências
Mais um passo para o usuário (tela de revisão), com mais segurança e controle. A IA não tem permissão de escrita.

## Alternativa descartada
Salvamento automático.
