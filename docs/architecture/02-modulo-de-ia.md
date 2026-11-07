## 7. Módulo de IA

A IA tem dois papéis separados: **extrair** dados de imagens e **interpretar** números já calculados. Ela nunca grava dados nem faz contas financeiras.

**Contrato de extração.** A resposta deve ser JSON validado pelo backend (schema rígido; resposta fora do schema é rejeitada). Como uma imagem pode ter várias transações, o formato é uma lista:

```json
{
  "transactions": [
    {
      "type": "EXPENSE",
      "amount": 85.90,
      "date": "2026-11-05",
      "merchant": "Supermercado",
      "category": "FOOD",
      "payment_method": "PIX",
      "description": "Compras de mercado",
      "confidence": 0.96,
      "missing_fields": [],
      "needs_review": false
    }
  ],
  "image_quality": "GOOD"
}
```

Regras: campo não identificado vem `null` e entra em `missing_fields`; o modelo não presume dados ausentes; `confidence` é uma estimativa do modelo, não garantia; o backend converte `amount` para `BigDecimal` e valida data, enums e categoria existente.

**Duplicidade (duas camadas)**

| Camada | Como detecta | Resultado |
| --- | --- | --- |
| Imagem | Hash SHA-256 do arquivo por usuário | Avisa que a imagem já foi enviada |
| Transação | Mesmo valor + data próxima + estabelecimento similar | Marca como possível duplicata na revisão |

A decisão final é sempre do usuário.

**Assistente financeiro.** O backend (1) classifica a pergunta, (2) executa consultas e cálculos em SQL/serviços, (3) envia à IA apenas os resultados agregados e (4) devolve a explicação. A IA não recebe o histórico completo e deve citar apenas números fornecidos.

**Registro e custo.** Cada chamada gera um `AIAnalysis` com status, modelo, tokens e o JSON validado (sem dados além do necessário), para auditoria, depuração e controle de custo. O provedor de IA fica atrás de uma interface (`VisionExtractor`, `FinancialAdvisor`) para permitir troca futura.

**Pontos a validar na Fase 4:** qual API multimodal usar, custo por imagem, política de retenção de dados do provedor e a taxa real de acerto com comprovantes brasileiros (Pix, extratos, cupons).

