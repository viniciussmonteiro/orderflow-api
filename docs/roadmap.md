# Roadmap — OrderFlow API

> **Estado do projeto:** MVP funcional, com código local preparado e validado para a versão `0.1.0`.
> **Publicação:** a tag `v0.1.0` e a primeira GitHub Release ainda não foram criadas. O repositório continua privado até a revisão final.

## Visão geral

O **OrderFlow API** é uma API REST de produtos, pedidos e estoque desenvolvida em Java 21, Spring Boot 4.1.2 e PostgreSQL 17. O MVP prioriza integridade de dados, consistência em operações concorrentes, testes automatizados e documentação técnica. A publicação do **código-fonte** no GitHub não significa disponibilizar a API como serviço em produção.

## v0.1.0 — MVP funcional (implementado)

### API e regras de negócio

- [x] Cadastro de produtos com SKU único, preço e estoque.
- [x] Listagem e consulta de produtos por ID.
- [x] Criação de pedidos com um ou mais itens e consulta por ID.
- [x] Cálculo de subtotais e total usando `BigDecimal`.
- [x] Registro de identificador, SKU, nome e preço do produto nos itens do pedido no momento da compra.
- [x] Validação de entradas com Jakarta Bean Validation.
- [x] Tratamento centralizado com `ProblemDetail` para validação (`400`), recursos inexistentes (`404`) e conflitos (`409`) contemplados pelos handlers.
- [x] Transação na criação de pedidos com rollback em caso de falha.
- [x] Reserva de estoque com `PESSIMISTIC_WRITE` e aquisição de bloqueios em ordem crescente de ID.

### Persistência, testes e documentação

- [x] PostgreSQL 17 configurado para execução local com Docker Compose.
- [x] Migração inicial com Flyway e validação do esquema pelo Hibernate (`ddl-auto: validate`).
- [x] **5 testes unitários** de produto e serviço de pedidos.
- [x] **6 testes de integração** com Spring MockMvc, Testcontainers e PostgreSQL real.
- [x] Teste de concorrência com duas compras disputando a única unidade disponível: uma retorna `201`, a outra `409`, sem venda acima do estoque.
- [x] OpenAPI 3.1 e Swagger UI para os cinco endpoints REST.
- [x] GitHub Actions com Java 21, `actions/checkout@v7`, `actions/setup-java@v6` e `mvn -B -ntp verify`.
- [x] Revisão das dependências Jackson, Logback e ASM e substituição do antigo `android-json` no escopo de testes.
- [x] Atualização de `net.minidev:json-smart` para `2.6.1` no escopo de testes, confirmada localmente com `mvn dependency:tree`.

### Evidências verificadas

- [x] `mvn clean verify` local na versão `0.1.0`: **11 testes aprovados, 0 falhas e JAR gerado**.
- [x] Build da imagem com `docker compose --profile api up --build -d`, incluindo o artefato `orderflow-api-0.1.0.jar`.
- [x] Execução simultânea dos contêineres da API e do banco; PostgreSQL com status `healthy`.
- [x] Inicialização da API com Flyway e requisição `GET /api/v1/products` respondendo `200 OK`.
- [x] Criação de produto e pedido usando a API em contêiner: pedido confirmado, cálculo correto e redução do estoque de 3 para 1 unidade.
- [x] Workflow Java CI aprovado no GitHub para o commit `50c28e5`, com 11 testes e empacotamento Maven.

> **Escopo da validação de concorrência:** o teste de duas requisições simultâneas demonstra o comportamento esperado nesse cenário. Ele não substitui testes de carga, análises extensivas de disputa ou testes de resiliência.
>
> **Escopo da validação do CI:** o workflow aprovado corresponde ao último commit publicado antes das alterações locais de preparação da `0.1.0`. O commit final da release ainda deverá passar novamente pelo CI.

## Preparação da primeira release pública (em andamento)

- [x] Atualizar o README com funcionalidades, arquitetura, testes e documentação da API.
- [x] Validar a execução da aplicação completa via Docker Compose e uma operação HTTP de ponta a ponta.
- [x] Atualizar o Java CI para as Actions adotadas e executar `mvn verify` com sucesso.
- [x] Alinhar localmente `pom.xml` e `Dockerfile` ao artefato `orderflow-api-0.1.0.jar`; metadados OpenAPI informam a versão `0.1.0`.
- [x] Validar localmente a atualização do `json-smart` para `2.6.1` e executar a suíte completa.
- [ ] Atualizar e revisar `README.md`, `docs/roadmap.md` e `docs/architecture.md` para a versão estável e para os resultados mais recentes.
- [ ] Ajustar `.env.example` para a porta `5433`, usada pelo PostgreSQL quando acessado a partir do host.
- [ ] Revisar arquivos rastreados, histórico Git e arquivos locais quanto a credenciais ou informações sensíveis; confirmar que as senhas de exemplo nunca foram usadas fora do ambiente local.
- [ ] Concluir o commit de preparação da `0.1.0` e publicá-lo na branch `main`.
- [ ] Confirmar o GitHub Actions **verde no commit final da release**.
- [ ] Criar e publicar a tag anotada `v0.1.0` a partir do commit aprovado.
- [ ] Publicar a GitHub Release com notas de versão e limitações conhecidas.
- [ ] Decidir se será incluída uma licença de software e verificar o conteúdo público uma última vez.
- [ ] Alterar a visibilidade do repositório para **público**, após a revisão final.
- [ ] Opcional: acrescentar capturas do Swagger, do CI e dos testes sem incluir informações sensíveis.

## v0.2 — Evolução de funcionalidades (planejada)

- [ ] Paginação e ordenação na listagem de produtos.
- [ ] Busca e filtros no catálogo, incluindo consulta por SKU.
- [ ] Atualização de produtos e gerenciamento controlado de estoque.
- [ ] Tratamento ampliado de JSON malformado, parâmetros inválidos e casos-limite.
- [ ] Ampliação dos testes de concorrência e cenários extremos.
- [ ] Diagrama entidade-relacionamento (ERD) e documentação complementar das decisões técnicas.

## v0.3 — Regras avançadas e segurança (planejada)

- [ ] Autenticação e autorização com Spring Security, caso o projeto evolua para um serviço com usuários.
- [ ] Contas, papéis e permissões.
- [ ] Idempotência na criação de pedidos.
- [ ] Cancelamento de pedidos com reposição segura de estoque.
- [ ] Histórico de movimentações de estoque.
- [ ] Observabilidade, limites de requisição e configuração específica por ambiente.

## v1.0 — Eventual operação como serviço público (fora do escopo do MVP)

- [ ] Definir hospedagem segura, gestão de segredos e banco de dados apropriado.
- [ ] Realizar revisão de segurança, testes de carga e validações de resiliência.
- [ ] Configurar monitoramento, logs e procedimentos operacionais.
- [ ] Disponibilizar uma demonstração hospedada apenas após implementar as proteções necessárias.

**Limitação atual:** o OrderFlow é um projeto de portfólio backend preparado para execução local. Não possui autenticação ou autorização e não deve ser disponibilizado na internet como serviço de produção sem medidas adicionais.

Consulte o [README](../README.md) e a [documentação de arquitetura](architecture.md).
