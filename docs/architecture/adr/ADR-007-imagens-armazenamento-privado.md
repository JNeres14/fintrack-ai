# ADR-007: Imagens em armazenamento privado com URL temporária

Status: aceita (documento técnico inicial, 06/11/2026)

## Contexto
Comprovantes são dados financeiros sensíveis (RNF-05). Onde hospedar e se o armazenamento será compatível com S3 ou disco privado ainda está em aberto.

## Decisão
Imagens ficam em armazenamento privado, com nome gerado (UUID), e o acesso é por URL temporária que expira em minutos. O banco guarda só a chave de armazenamento.

## Consequências
Privacidade dos comprovantes e banco leve. Exige um módulo storage e a escolha do armazenamento antes da Fase 4.

## Alternativa descartada
Arquivos públicos ou imagens dentro do banco.
