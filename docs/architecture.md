# Arquitetura inicial

Monolito modular pequeno: `product`, `order`, `shared`.

HTTP request -> Controller (@Valid) -> Service (@Transactional)
-> Spring Data Repository -> PostgreSQL

- `product`: cadastro e regra de reserva de estoque.
- `order`: pedido e snapshot imutavel no item (preco e nome no momento da venda).
- `shared`: erros de dominio para respostas HTTP padronizadas.
- Flyway: schema versionado; Hibernate configurado para `validate`.

## Decisões
- `BigDecimal` para dinheiro; nao usar float/double.
- Produtos travados com `PESSIMISTIC_WRITE` em ordem crescente de ID para reduzir race conditions.
- Uma transação garante atomicidade de retirada do estoque + persistência do pedido.
- A primeira versao rejeita um mesmo produto duplicado no payload.
- Esta versao nao possui autenticacao: API apenas para laboratorio local.

## Limites conhecidos
Nao foi executada uma bateria de testes de concorrencia com um PostgreSQL real neste pacote inicial. Nao exponha a API publicamente sem seguranca, limites de requisicao e revisao de configuracoes.
