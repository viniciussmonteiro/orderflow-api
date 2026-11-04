# OrderFlow API

[![Java CI](https://github.com/viniciussmonteiro/orderflow-api/actions/workflows/ci.yml/badge.svg)](https://github.com/viniciussmonteiro/orderflow-api/actions/workflows/ci.yml)

API REST para **gerenciamento de produtos, criação de pedidos e controle de estoque**, desenvolvida com **Java 21, Spring Boot 4 e PostgreSQL 17**.

O principal desafio abordado pelo projeto é a **consistência de operações transacionais**: uma compra deve validar os itens, calcular os valores e reservar o estoque de forma atômica, inclusive quando diferentes requisições tentam adquirir o mesmo produto simultaneamente.

**Estado atual:** MVP funcional, versão de desenvolvimento `0.1.0-SNAPSHOT`. A API foi validada localmente e por uma pipeline de integração contínua no GitHub Actions.

## Destaques técnicos

- **Consistência transacional:** criação de pedidos com `@Transactional`, garantindo rollback quando uma operação não pode ser concluída.
- **Proteção contra venda acima do estoque:** bloqueio pessimista (`PESSIMISTIC_WRITE`) durante a reserva de produtos, com aquisição de bloqueios em ordem crescente de ID.
- **Cálculos monetários precisos:** uso de `BigDecimal` para preços, subtotais e total dos pedidos.
- **Histórico preservado:** os itens do pedido armazenam um registro dos dados do produto e do preço no momento da compra.
- **Validação e tratamento de erros:** Bean Validation e respostas HTTP padronizadas com `ProblemDetail` para erros `400`, `404` e `409`.
- **Qualidade automatizada:** 11 testes unitários e de integração, incluindo concorrência real sobre PostgreSQL com Testcontainers.

## Tecnologias

| Área | Tecnologias |
| --- | --- |
| Linguagem e framework | Java 21, Spring Boot 4.1.2, Spring Web MVC |
| Persistência | Spring Data JPA, Hibernate, PostgreSQL 17 |
| Migrações e validação | Flyway, Jakarta Bean Validation |
| Testes | JUnit, Mockito, Spring MockMvc, Testcontainers |
| Documentação | OpenAPI 3.1, springdoc-openapi, Swagger UI |
| Ferramentas e automação | Maven, Docker, Docker Compose, Git, GitHub Actions |

## Funcionalidades implementadas

- Cadastrar produtos com SKU, nome, preço e quantidade em estoque.
- Listar produtos cadastrados e consultar um produto por ID.
- Criar pedidos com um ou mais itens e consultar pedidos por ID.
- Calcular subtotais e total, preservando os valores registrados nos itens dos pedidos.
- Validar disponibilidade de estoque antes de confirmar uma compra.
- Rejeitar pedidos inválidos, produtos inexistentes, SKUs duplicados e tentativas de compra sem estoque suficiente.
- Atualizar o estoque dentro da mesma transação que registra o pedido.

## Arquitetura e decisões de projeto

A aplicação utiliza uma arquitetura em camadas, organizada por domínio (`product`, `order` e `shared`):

```text
Requisição HTTP
    ↓
Controller → validação de entrada
    ↓
Service → regras de negócio e transações
    ↓
Repository → Spring Data JPA / Hibernate
    ↓
PostgreSQL
```

Na criação de pedidos, os produtos são consultados com bloqueio pessimista e processados em ordem crescente de ID. Essa abordagem reduz disputas de concorrência e evita que duas compras confirmem a mesma unidade disponível. Se algum item não puder ser processado, a transação é revertida, preservando a consistência do estoque e dos pedidos.

O esquema do banco é versionado com **Flyway**, enquanto as falhas de negócio e validação são traduzidas para respostas HTTP por um tratamento centralizado de exceções.

Detalhes adicionais estão disponíveis em [docs/architecture.md](docs/architecture.md).

## Endpoints REST

| Método | Endpoint | Descrição | Respostas principais |
| --- | --- | --- | --- |
| `POST` | `/api/v1/products` | Cadastrar produto | `201`, `400`, `409` |
| `GET` | `/api/v1/products` | Listar produtos | `200` |
| `GET` | `/api/v1/products/{id}` | Consultar produto | `200`, `404` |
| `POST` | `/api/v1/orders` | Criar pedido | `201`, `400`, `404`, `409` |
| `GET` | `/api/v1/orders/{id}` | Consultar pedido | `200`, `404` |

### Documentação interativa

Com a aplicação em execução, a especificação OpenAPI e os endpoints podem ser explorados diretamente pelo navegador:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

A documentação apresenta os contratos de entrada e saída, as operações disponíveis e os formatos das respostas de erro.

## Execução local

**Pré-requisitos:** JDK 21, Maven 3.9+ e Docker com Docker Compose.

1. Iniciar o banco PostgreSQL:

   ```bash
   docker compose up -d db
   ```

2. Iniciar a API:

   ```bash
   mvn spring-boot:run
   ```

3. Acessar a [documentação Swagger](http://localhost:8080/swagger-ui/index.html) ou consultar `http://localhost:8080/api/v1/products`.

A API utiliza a porta **8080**. O PostgreSQL é publicado apenas no endereço local `127.0.0.1:5433`, encaminhado para a porta `5432` do contêiner. As credenciais configuradas no Compose destinam-se exclusivamente ao desenvolvimento local.

### Exemplo de utilização (PowerShell)

Cadastrar um produto:

```powershell
$produto = @{
    sku   = 'DEMO-BOOK-001'
    name  = 'Livro Java'
    price = 59.90
    stock = 10
} | ConvertTo-Json

$novoProduto = Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/products' `
    -ContentType 'application/json' `
    -Body $produto

$novoProduto
```

Criar um pedido com duas unidades do produto cadastrado:

```powershell
$pedido = @{
    customerName = 'Cliente Exemplo'
    items = @(
        @{ productId = $novoProduto.id; quantity = 2 }
    )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/orders' `
    -ContentType 'application/json' `
    -Body $pedido
```

Nesse cenário, o pedido tem total de **R$ 119,80** e o estoque do produto passa de **10 para 8 unidades**. Para repetir o cadastro, utilize um SKU diferente, pois o campo é único.

## Testes e integração contínua

A suíte atual contém **11 testes automatizados**:

| Tipo | Quantidade | Cenários abordados |
| --- | ---: | --- |
| Unitários | 5 | Regras de produto e serviço de pedidos |
| Integração | 6 | Criação de pedidos, atualização e insuficiência de estoque, rollback, validação, SKU duplicado e concorrência |

O teste de concorrência dispara **duas requisições simultâneas** para um produto com apenas uma unidade disponível. O cenário verifica que somente uma compra retorna `201 Created`, a outra retorna `409 Conflict`, o estoque termina em zero e apenas um pedido é persistido.

Os testes de integração utilizam **PostgreSQL real em contêiner temporário**, iniciado pelo Testcontainers. Por isso, o Docker precisa estar ativo durante sua execução:

```bash
mvn clean test
```

A pipeline [Java CI](https://github.com/viniciussmonteiro/orderflow-api/actions/workflows/ci.yml) executa `mvn test` automaticamente em pushes e pull requests. A execução que incorporou a documentação OpenAPI foi concluída com sucesso.

## Estrutura do projeto

```text
src/main/java/dev/vinicius/orderflow/
├── product/                 # Entidade, DTOs, endpoints e serviços de produtos
├── order/                   # Pedidos, itens, DTOs e regras transacionais
├── shared/                  # Exceções e respostas de erro
└── OrderFlowApplication.java
src/main/resources/
├── db/migration/            # Migrações versionadas com Flyway
└── application.yml          # Configurações da aplicação
src/test/java/              # Testes unitários e de integração
.github/workflows/ci.yml    # Pipeline de integração contínua
compose.yaml                # PostgreSQL e perfil opcional da API
Dockerfile                  # Build da aplicação em contêiner
README.md                   # Apresentação e execução do projeto
docs/                       # Arquitetura e planejamento
```

## Escopo e evolução

O OrderFlow é um **projeto de portfólio backend**. A versão atual prioriza integridade de dados, testes automatizados e documentação de API, e não deve ser interpretada como um serviço pronto para produção.

A aplicação ainda **não possui autenticação ou autorização** e foi preparada para utilização local. Entre as evoluções planejadas estão paginação e filtros no catálogo, cancelamento com reposição de estoque, idempotência na criação de pedidos e mecanismos de segurança para uma eventual publicação.

A revisão de dependências incluiu atualizações de segurança para Jackson, Logback e ASM e a substituição da antiga biblioteca Android JSON. Permanece em avaliação um alerta associado ao `json-smart`, dependência transitiva utilizada **somente no escopo de testes**; isso não representa uma correção definitiva da vulnerabilidade.

O planejamento incremental está disponível em [docs/roadmap.md](docs/roadmap.md).

## Autor

**[Vinicius Monteiro](https://github.com/viniciussmonteiro)** — desenvolvimento Java Backend.
