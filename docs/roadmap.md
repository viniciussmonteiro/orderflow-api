# Roadmap incremental

## v0.1 — MVP entregue como codigo-base
- [x] CRUD parcial de produtos: criacao e leitura
- [x] Criacao e leitura de pedidos
- [x] Calculo de totais com BigDecimal
- [x] Bloqueio pessimista de produtos durante pedidos
- [x] PostgreSQL, Flyway, Docker Compose
- [x] Testes unitarios iniciais e pipeline CI definidos
- [ ] Rodar Maven e testes no ambiente de desenvolvimento
- [ ] Exercitar a API contra PostgreSQL com Docker

## v0.2 — Qualidade
- [ ] Testes de integracao com Testcontainers/PostgreSQL
- [ ] Paginação, filtros e busca por SKU
- [ ] Tratamento de JSON malformado, parametros e limites de dados
- [ ] Swagger/OpenAPI
- [ ] Teste concorrente de duas compras sobre o mesmo produto

## v0.3 — Segurança e regras completas
- [ ] Spring Security / OAuth2 Resource Server
- [ ] Contas, papeis e permissoes
- [ ] Idempotencia na criacao do pedido
- [ ] Cancelamento com reposicao segura de estoque
- [ ] Historico de movimentacoes de estoque

## v1.0 — Entrega para portfólio
- [ ] Demo hospedada
- [ ] Documentação de arquitetura e ERD
- [ ] Evidências de testes e screenshots
- [ ] Revisão de seguranca, carga e observabilidade
