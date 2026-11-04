# Arquitetura — OrderFlow API

## 1. Visão geral

O **OrderFlow API** é um monólito modular de pequeno porte para gerenciamento de produtos, pedidos e estoque. Foi desenvolvido com **Java 21, Spring Boot 4.1.2, Spring Data JPA, PostgreSQL 17 e Flyway**, priorizando consistência transacional, regras de negócio explícitas e testes automatizados.

O código foi preparado e validado localmente para a versão **`0.1.0`**. A criação da tag `v0.1.0` e da GitHub Release ainda integra a etapa de publicação. O software destina-se à execução local: disponibilizar o repositório publicamente **não** significa disponibilizar a API como serviço público.

## 2. Organização e fluxo de requisições

```text
Cliente HTTP / Swagger UI
          |
          v
@RestController (product / order)
          | @Valid e DTOs de entrada
          v
@Service (regras de negócio)
          | @Transactional na criação de pedidos
          v
Spring Data JPA (repositories)
          | Hibernate + bloqueio pessimista na reserva de estoque
          v
PostgreSQL 17
          ^
          | Migrações SQL versionadas pelo Flyway
```

| Módulo | Responsabilidade | Principais componentes |
| --- | --- | --- |
| `product` | Cadastro, consulta, validação de SKU e reserva de estoque | `Product`, `ProductDto`, `ProductController`, `ProductService`, `ProductRepository` |
| `order` | Criação e consulta de pedidos, itens, totalização e transações | `CustomerOrder`, `OrderItem`, `OrderDto`, `OrderController`, `OrderService`, `OrderRepository` |
| `shared` | Exceções de domínio e mapeamento para respostas HTTP | `BusinessConflictException`, `ResourceNotFoundException`, `ApiExceptionHandler` |

O código de produção encontra-se em `src/main/java/dev/vinicius/orderflow/`. As migrações SQL estão em `src/main/resources/db/migration/`.

## 3. Modelo de dados

A migração `V1__create_orderflow_tables.sql` define três tabelas:

| Tabela | Campos e restrições relevantes |
| --- | --- |
| `products` | `id` (PK), `sku` (único), `name`, `price` (positivo), `stock` (não negativo) |
| `customer_orders` | `id` (PK), `customer_name`, `status`, `total` (não negativo), `created_at` |
| `order_items` | `id` (PK), `order_id` (FK para `customer_orders`), `product_id`, `sku`, `product_name`, `unit_price`, `quantity` (positiva), `subtotal` |

- Um pedido possui vários itens, relacionados por `order_items.order_id`.
- Cada item registra **um snapshot** do identificador, SKU, nome e preço do produto no momento da compra. A leitura posterior de um pedido não depende do preço atual do catálogo.
- `order_items.product_id` armazena o identificador do produto, **sem uma foreign key para `products`** na migração atual.
- O banco aplica restrições de unicidade e valores, complementando as validações feitas na API.
- O **Flyway** controla as mudanças do esquema e o **Hibernate** usa `ddl-auto: validate`, sem criar nem modificar tabelas automaticamente.

Na versão atual, os pedidos são criados com o estado `CONFIRMED`. Cancelamento e outros estados fazem parte de evoluções planejadas.

## 4. Criação de pedidos e consistência transacional

O endpoint `POST /api/v1/orders` utiliza `OrderService.create()` com `@Transactional`:

1. O DTO é validado na camada HTTP: nome do cliente obrigatório, lista não vazia e quantidades positivas.
2. O serviço rejeita itens com `productId` repetido no mesmo pedido.
3. Os itens são ordenados por `productId`, estabelecendo uma ordem consistente para aquisição dos bloqueios.
4. `ProductRepository.findByIdForUpdate()` consulta cada produto com `@Lock(PESSIMISTIC_WRITE)`.
5. `Product.reserveStock(quantity)` verifica o saldo e reduz a quantidade disponível. `OrderItem` registra os dados da compra e calcula o subtotal usando `BigDecimal`.
6. `CustomerOrder` acumula o total e é persistido junto com os itens.
7. A operação bem-sucedida retorna `201 Created`. Se ocorrer uma exceção de negócio ou recurso inexistente, a transação é revertida.

### Por que usar bloqueio pessimista?

Duas compras não podem confirmar simultaneamente a única unidade disponível. O bloqueio de linha no PostgreSQL serializa a modificação daquele produto; a transação concorrente encontra o estoque atualizado ao adquirir o bloqueio. A ordem crescente de IDs ajuda a **reduzir**, mas não elimina universalmente, o risco de deadlocks.

O teste de concorrência executa duas requisições coordenadas com threads e `CountDownLatch` e verifica:

- uma requisição com `201 Created` e outra com `409 Conflict`;
- estoque final igual a zero;
- somente um pedido novo persistido.

O teste usa PostgreSQL real via Testcontainers. Trata-se de uma evidência para o cenário definido, **não de um teste de estresse nem de uma garantia contra todas as condições de corrida**.

## 5. Contratos HTTP e tratamento de erros

A API expõe cinco endpoints sob o prefixo `/api/v1`:

| Método | Rota completa | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/products` | Cadastrar produto |
| `GET` | `/api/v1/products` | Listar produtos |
| `GET` | `/api/v1/products/{id}` | Consultar produto |
| `POST` | `/api/v1/orders` | Criar pedido |
| `GET` | `/api/v1/orders/{id}` | Consultar pedido |

A entrada usa Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Min`, `@Size` etc.) e regras de negócio nos serviços. O `ApiExceptionHandler`, baseado em `@RestControllerAdvice`, retorna `ProblemDetail` nos casos tratados:

- **`400 Bad Request`** — falhas de validação do corpo da requisição (`MethodArgumentNotValidException`);
- **`404 Not Found`** — produto ou pedido inexistente;
- **`409 Conflict`** — estoque insuficiente, conflitos de negócio ou violações de integridade contempladas pelo handler.

Casos adicionais, como respostas customizadas para JSON malformado, permanecem no roadmap. Os esquemas OpenAPI têm nomes específicos para evitar colisões entre os DTOs internos `Create` e `View`.

A documentação interativa está disponível **com a API rodando localmente**:

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [Especificação OpenAPI JSON](http://localhost:8080/v3/api-docs)

## 6. Testes e integração contínua

A suíte contém **11 testes automatizados**:

- **5 unitários:** regras da entidade `Product` e do serviço `OrderService`.
- **6 de integração:** criação de pedido e redução de estoque; estoque insuficiente; rollback por produto inexistente; quantidade zero; SKU duplicado; compras concorrentes.

Os testes de integração usam `@SpringBootTest`, Spring MockMvc, Testcontainers e PostgreSQL `17-alpine`, com banco temporário. Para executá-los localmente, o Docker precisa estar disponível.

```bash
mvn clean verify
```

O workflow `.github/workflows/ci.yml` utiliza **Java 21**, `actions/checkout@v7` e `actions/setup-java@v6`, executando `mvn -B -ntp verify` em pushes para `main` e em pull requests. A execução referente ao commit **`50c28e5`** foi aprovada com 11 testes, 0 falhas e geração de JAR executável. As alterações locais preparatórias da versão `0.1.0` deverão ser validadas novamente pelo CI depois do respectivo commit.

## 7. Execução local e contêineres

- `application.yml`: porta `8080`; PostgreSQL padrão em `localhost:5433`, configurável via `DB_URL`, `DB_USER` e `DB_PASSWORD`.
- `compose.yaml` (`db`): PostgreSQL `17-alpine`, healthcheck, volume persistente e porta publicada em `127.0.0.1:5433`.
- `compose.yaml` (`api`): serviço habilitado com o perfil `api`; usa `db:5432` na rede interna e depende do banco saudável.
- `Dockerfile`: build em dois estágios (Maven e Java 21 JRE), com execução por usuário não privilegiado. O artefato da versão estável local é `orderflow-api-0.1.0.jar`.

Para iniciar os dois serviços:

```bash
docker compose --profile api up --build -d
```

A imagem da API foi construída com sucesso e o PostgreSQL ficou saudável. A aplicação inicializou, validou a migração do Flyway e respondeu `200 OK` na consulta de produtos. Em teste de ponta a ponta, um pedido de duas unidades foi confirmado e o estoque passou de três para uma unidade.

Para encerrar sem excluir o volume de dados:

```bash
docker compose --profile api down
```

**Atenção:** as credenciais de `compose.yaml` e `application.yml` são exemplos exclusivos de desenvolvimento. A porta `5433` é usada pelo host; a porta `5432` é usada entre contêineres. O arquivo `.env.example` deve refletir a porta do host antes da publicação. Não utilizar essas configurações em produção.

## 8. Decisões técnicas, segurança e limitações

### Decisões

- `BigDecimal` para valores monetários.
- Snapshot dos dados de produto nos itens do pedido.
- `@Transactional` e `PESSIMISTIC_WRITE` na criação de pedidos com reserva de estoque.
- Migrações geridas pelo Flyway e esquema validado pelo Hibernate.
- Organização por domínio, mantendo os elementos relacionados próximos no código.
- OpenAPI/Swagger UI para documentação dos contratos HTTP.

### Dependências

Foram revisadas as versões de Jackson, Logback e ASM, além da substituição do antigo `android-json` utilizado nos testes. A dependência transitiva **`net.minidev:json-smart`**, previamente identificada na versão `2.6.0` por conta do alerta **CVE-2026-113602**, foi atualizada localmente para **`2.6.1`** no escopo de testes. A nova versão foi confirmada por `mvn dependency:tree` e a suíte passou com 11 testes. Isso não equivale a uma auditoria completa de todas as dependências.

### Limitações e trabalho futuro

- **Sem autenticação ou autorização:** a API foi preparada para execução local e não deve ser exposta como serviço público sem controles de segurança.
- **Sem cancelamento de pedidos, idempotência ou histórico de movimentações de estoque.**
- **Sem paginação ou filtros avançados** na listagem de produtos.
- **Sem testes de carga extensivos**; o teste de concorrência cobre um cenário controlado.
- **Publicação ainda pendente:** atualizar os documentos, revisar credenciais e histórico, registrar o commit `0.1.0`, validar o CI desse commit e publicar a tag/release antes de mudar a visibilidade do repositório.

Consulte o [README](../README.md) e o [roadmap](roadmap.md) para instruções e próximas entregas.
