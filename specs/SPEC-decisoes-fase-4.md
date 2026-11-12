# SPEC — Decisões da Fase 4 (extração por IA, imagens, celular e escopo do MVP)

Data: 2026-11-09. Status: decisões aprovadas em conversa (rodadas de perguntas do Grill Me); aguardando revisão deste arquivo. Origem: dúvidas em aberto da seção 9 do SPEC.md.

## 1. Premissas (ditas por mim)
- Uso meus comprovantes reais, então privacidade é requisito.
- Custo alvo: R$ 0 (só gratuito ou local).
- Prints e fotos são o formato dominante dos comprovantes.
- O app deve ser usado pelo celular, além do PC.
- Hardware do PC (notebook): Intel i5-13420H (8 núcleos), 16 GB de RAM, NVIDIA RTX 3050 Laptop com 6 GB de VRAM dedicada.

## 2. Decisões

### 2.1 Extração por IA: local por padrão
- `VisionExtractor` continua sendo a interface (ADR-006).
- Implementação padrão: extração na máquina do usuário, sem enviar imagem para fora.
- Implementação por API: opcional, **desligada por padrão**, ativada por configuração, só depois de ler a política de dados do provedor.
- Testes automatizados usam a implementação fake.
- Primeira tentativa: OCR clássico com regras (leve, roda em qualquer máquina).
- Plano B: modelo de visão pequeno local, só se o OCR ficar abaixo do limiar. O hardware comporta modelos quantizados pequenos na GPU de 6 GB; a RAM de 16 GB já fica bastante usada por IDE, banco e backend, então o modelo e o backend não devem rodar a pleno ao mesmo tempo.
- Plano C: híbrido (texto extraído localmente vai ao modelo, a imagem não), com ADR.

### 2.2 Medição de acerto (início da Fase 4)
- Amostra de 20 a 30 comprovantes reais.
- Gabarito digitado por mim em CSV local, fora do Git (ignorado pelo `.gitignore`). Nenhuma imagem real e nenhum gabarito entram no Git ou em fixtures.
- Regras de contagem: valor e data exigem igualdade exata após normalizar o formato (R$ 12,5 = 12,50); estabelecimento conta como certo se um humano o reconhece (tolera abreviações); tipo (entrada ou saída) exige igualdade exata.
- Limiar: 85% de acerto em valor e data para o MVP. Isso só vale porque a extração **nunca grava sozinha**: o resultado é um rascunho que eu confirmo ou corrijo. Se um dia houver gravação automática, o limiar sobe para 95%.

### 2.3 Rascunho e extração assíncrona
- A imagem é salva e a extração roda de forma assíncrona; o envio exige backend alcançável (sem fila offline no celular no MVP).
- Estados do rascunho: `PENDENTE` (imagem salva, extração em andamento), `PRONTO` (extraído, aguardando confirmação), `FALHOU` (sem leitura, preencher à mão), `EXPIRADO` (passou de 30 dias, imagem apagada). Confirmar vira transação normal e sai da lista de rascunhos.
- Extração falhou ou veio vazia: a imagem fica salva, o rascunho aparece com campos vazios e o aviso "não consegui ler". Nunca se descarta a imagem sozinho nem se inventa valor. A falha vai para o log sem o conteúdo do comprovante.

### 2.4 Imagens e armazenamento
- Módulo `storage` grava em pasta privada fora do repositório, nome UUID, URL temporária gerada pelo backend, atrás de uma interface (compatível com ADR-007).
- O caminho vem de variável de ambiente (valor no `.env`, que não vai ao Git), com padrão em pasta do meu usuário fora de `Documents\PROJETOS`. O backend falha ao iniciar se a pasta não existir ou não for gravável. Criptografia em disco fica fora do MVP.
- Retenção: rascunho não confirmado expira em 30 dias e a imagem é apagada junto. Imagem de transação confirmada fica enquanto a transação existir e é apagada se eu excluir a transação.
- Limite de 10 MB por imagem, validado no backend pelo tipo real do arquivo (não só pela extensão). Acima disso, erro claro e nada é gravado.

### 2.5 Escopo do MVP
- Formatos aceitos no backend: JPEG, PNG e WebP. PDF fora do MVP (entra depois, começando por PDF com texto).
- Só BRL, sem coluna de moeda. Outra moeda exige migração Flyway nova e ADR.

### 2.6 Celular
- No MVP o celular funciona só em casa, no mesmo Wi-Fi do PC. Fora de casa entra na Fase 6, por túnel gratuito até o PC (por exemplo Tailscale), sem mover o backend para a nuvem, para a extração continuar local e o custo R$ 0.
- HTTPS: o navegador só libera câmera e instalação do PWA em conexão segura, e o endereço da rede local não conta. Tratar na Fase 3 com certificado local de desenvolvimento (por exemplo mkcert instalado no iPhone) ou o HTTPS gratuito do Tailscale, registrando em ADR. Enquanto isso, upload por seleção de arquivo da galeria funciona sem HTTPS.
- Fotos de iPhone saem em HEIC. O campo de upload do PWA pede `image/*` para o navegador converter para JPEG; testar com foto real no início da Fase 3. Se a conversão falhar, ela vira tarefa própria da Fase 4.

## 3. Riscos
- Qualidade do OCR/modelo local pode ficar abaixo do necessário; mitigação: medição inicial e planos B e C.
- O híbrido aumenta a complexidade e a superfície de privacidade (texto sai da máquina).
- Imagens reais contêm dados sensíveis: nunca versionar, nunca usar em fixtures de teste.
- O PC é um notebook: se dormir ou ficar sem rede, o celular não consegue enviar. Mitigação no MVP: usar em casa com o notebook ligado; avaliar a política de energia na Fase 3.
- Memória: modelo de visão local, backend, banco e IDE disputam 16 GB de RAM.
- HEIC do iPhone pode exigir conversão própria.

## 4. Fora desta spec
Código, migrações, escolha da biblioteca de OCR ou do modelo (feita na medição da Fase 4) e commits.
