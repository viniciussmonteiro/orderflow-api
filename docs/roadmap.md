# Roadmap — OrderFlow API

> **Estado do projeto:** MVP funcional, na versão de desenvolvimento `0.1.0-SNAPSHOT`.
> As seções abaixo descrevem funcionalidades implementadas e próximas entregas; **não** representam versões já publicadas no GitHub Releases.

## Visão geral

O OrderFlow é uma API REST de produtos, pedidos e estoque. O núcleo funcional foi implementado e validado com PostgreSQL, testes automatizados e integração contínua. O objetivo imediato é concluir a **primeira entrega de portfólio**, sem confundi-la com uma implantação pronta para produção.

## v0.1 — MVP funcional (implementado)

### API e regras de negócio

- [x] Cadastro de produtos com SKU único, preço e estoque.
- [x] Listagem e consulta de produtos por ID.
- [x] Criação de pedidos com um ou mais itens e consulta de pedidos por ID.
- [x] Cálculo de subtotais e total usando `BigDecimal`.
- [x] Registro de SKU, nome e preço do produto nos itens do pedido no momento da compra.
- [x] Validação de entrada com Jakarta Bean Validation.
- [x] Respostas HTTP para erros de validação (`400`), recursos inexistentes (`404`) e conflitos (`409`), com `ProblemDetail`.
- [x] Transação na criação de pedidos e rollback quando a operação falha.
- [x] Reserva de estoque com bloqueio pessimista (`PESSIMISTIC_WRITE`) e ordenação de bloqueios por ID.

### Banco, testes e documentação

- [x] PostgreSQL 17 com configuração local via Docker Compose.
- [x] Migração inicial do esquema usando Flyway e validação do modelo pelo Hibernate.
- [x] **5 testes unitários** de produto e serviço de pedidos.
- [x] **6 testes de integração** com Spring MockMvc e PostgreSQL real via Testcontainers.
- [x] Teste de duas compras concorrentes disputando uma unidade em estoque: uma confirmação e um conflito.
- [x] Pipeline Java CI no GitHub Actions, executando `mvn test` em push e pull request.
- [x] OpenAPI 3.1 e Swagger UI para os cinco endpoints REST, incluindo esquemas e respostas HTTP documentadas.
- [x] Revisão de dependências: atualização de Jackson, Logback e ASM; substituição do `android-json` antigo no escopo de testes.

### Evidências de validação

- [x] Execução local da suíte completa com **11 testes aprovados**.
- [x] Fluxos HTTP exercitados contra PostgreSQL local: cadastro, pedido confirmado, redução de estoque e rejeição por estoque insuficiente.
- [x] GitHub Actions aprovado para o commit que incorporou a documentação OpenAPI (`08a9096`).

> **Limite do teste de concorrência:** o cenário de duas requisições foi aprovado, mas não substitui testes de carga, repetição intensiva ou análise de todos os padrões de disputa.

## Preparação da primeira release de portfólio (em andamento)

- [ ] Publicar a atualização de `docs/roadmap.md` e `docs/architecture.md` junto ao README já revisado.
- [ ] Validar o **build da imagem da API** e sua execução com o banco via `docker compose --profile api up --build -d`.
- [ ] Conferir a persistência dos dados e o encerramento seguro dos contêineres; não executar `docker compose down -v` sem intenção de apagar o volume.
- [ ] Revisar `.github/workflows/ci.yml`: atualizar as versões de `actions/checkout` e `actions/setup-java` conforme as versões suportadas; conferir o Java 21 e a execução dos testes de integração.
- [ ] Confirmar novamente o GitHub Actions após a revisão do CI e do Docker.
- [ ] Revisar dados de desenvolvimento, configurações e documentação para impedir exposição acidental de credenciais ou da API sem autenticação.
- [ ] Anexar evidências de execução (Swagger UI, testes e GitHub Actions), sem incluir informações sensíveis.
- [ ] Harmonizar a versão do `pom.xml`, do `Dockerfile` e dos metadados OpenAPI antes de criar uma tag estável (por exemplo, `v0.1.0`).

### Risco de dependência ainda em avaliação

- [ ] Reavaliar a vulnerabilidade **CVE-2026-113602** no `net.minidev:json-smart:2.6.0`, dependência transitiva atualmente limitada ao escopo de testes. A revisão ainda não identificou uma versão corrigida adequada; o alerta **não está resolvido** e não deve ser ocultado sem justificativa.

## v0.2 — Evolução de funcionalidades (planejada)

- [ ] Paginação e ordenação na listagem de produtos.
- [ ] Busca e filtros no catálogo, incluindo busca por SKU.
- [ ] Atualização de produtos e gerenciamento controlado de estoque.
- [ ] Cobertura adicional de JSON malformado, parâmetros inválidos e limites de entrada.
- [ ] Ampliação dos testes de concorrência e casos extremos.
- [ ] Documentação adicional de modelo de dados (ERD) e decisões arquiteturais.

## v0.3 — Regras avançadas e segurança (planejada)

- [ ] Autenticação e autorização com Spring Security, caso o projeto evolua para um serviço com usuários.
- [ ] Contas, papéis e permissões.
- [ ] Idempotência na criação de pedidos.
- [ ] Cancelamento de pedidos com reposição segura de estoque.
- [ ] Histórico de movimentações de estoque.
- [ ] Revisão de observabilidade, limites de requisição e políticas de configuração por ambiente.

## v1.0 — Possível evolução para operação pública (futura, fora do escopo do MVP)

- [ ] Definir estratégia segura de hospedagem, credenciais e banco gerenciado.
- [ ] Executar revisão de segurança e testes de carga em ambiente apropriado.
- [ ] Configurar monitoramento, logs e procedimentos de operação.
- [ ] Publicar uma demonstração somente após implementar as proteções necessárias.

**Escopo atual:** o OrderFlow é um projeto de portfólio backend, preparado para execução local. Não possui autenticação nem requisitos suficientes para exposição pública em produção.

Consulte também o [README](../README.md) e a [documentação de arquitetura](architecture.md).
