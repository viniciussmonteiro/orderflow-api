# Arquitetura — OrderFlow API

## 1. Visão geral

O **OrderFlow API** é um monólito modular de pequeno porte para gerenciamento de produtos, pedidos e estoque. Foi desenvolvido com **Java 21, Spring Boot 4.1.2, Spring Data JPA, PostgreSQL 17 e Flyway**, priorizando consistência transacional, regras de negócio explícitas e testes automatizados.

O projeto encontra-se na versão de desenvolvimento `0.1.0-SNAPSHOT`. É um **MVP de portfólio para uso local**, não um serviço preparado para exposição pública.

## 2. Organização e fluxo de requisições

```text
Cliente HTTP / Swagger UI
        |
        v
@RestController (product / order)
        |  @Valid e DTOs de entrada
        v
@Service (regras de negócio)
        |  @Transactional na criação de pedidos
        v
Spring Data JPA (repositories)
        |  Hibernate + bloqueio pessimista quando necessário
        v
PostgreSQL 17
        ^
        |  Migrações SQL versionadas pelo Flyway
```

| Módulo | Responsabilidade | Componentes principais |
| --- | --- | --- |
| `product` | Cadastro, consulta, validação de SKU e reserva de estoque | `Product`, `ProductDto`, `ProductController`, `ProductService`, `ProductRepository` |
| `order` | Criação e consulta de pedidos, itens, totalização e transações | `CustomerOrder`, `OrderItem`, `OrderDto`, `OrderController`, `OrderService`, `OrderRepository` |
| `shared` | Exceções de domínio e mapeamento de falhas para respostas HTTP | `BusinessConflictException`, `ResourceNotFoundException`, `ApiExceptionHandler` |

O código de produção está em `src/main/java/dev/vinicius/orderflow/`. O esquema de banco é mantido em `src/main/resources/db/migration/`.

## 3. Modelo de dados

A migração inicial `V1__create_orderflow_tables.sql` cria três tabelas:

| Tabela | Campos e restrições relevantes |
| --- | --- |
| `products` | `id` (PK), `sku` (único), `name`, `price` (positivo), `stock` (não negativo) |
| `customer_orders` | `id` (PK), `customer_name`, `status`, `total` (não negativo), `created_at` |
| `order_items` | `id` (PK), `order_id` (FK para `customer_orders`), `product_id`, `sku`, `product_name`, `unit_price`, `quantity`, `subtotal` |

- Um pedido possui vários itens; o relacionamento é persistido por `order_items.order_id`.
- Cada item guarda **um snapshot** do identificador, SKU, nome e preço do produto no momento da compra. Assim, a leitura do pedido não depende de consultar o preço atual do catálogo.
- O campo `order_items.product_id` é armazenado como identificador do produto, **sem uma foreign key para `products` na migração atual**.
- O banco aplica restrições de unicidade e de valores, além das validações feitas na camada HTTP.
- O Flyway administra as migrações; o Hibernate está configurado com `ddl-auto: validate`, sem criar nem modificar o esquema automaticamente.

Na versão atual, o estado existente de um pedido é `CONFIRMED`; cancelamento e outros estados permanecem no roadmap.

## 4. Criação de pedidos e consistência transacional

A operação `POST /api/v1/orders` é executada por `OrderService.create()` dentro de uma transação Spring (`@Transactional`):

1. O DTO é validado na entrada (`customerName` obrigatório, itens não vazios e quantidades positivas).
2. O serviço rejeita produtos duplicados no mesmo pedido.
3. Os itens são ordenados por `productId`, estabelecendo uma ordem de aquisição de bloqueios mais previsível.
4. Para cada item, `ProductRepository.findByIdForUpdate()` consulta o produto com `@Lock(PESSIMISTIC_WRITE)`.
5. `Product.reserveStock(quantity)` verifica a disponibilidade e reduz o estoque; `OrderItem` registra o snapshot e calcula seu subtotal com `BigDecimal`.
6. `CustomerOrder` acumula o total e é persistido com os itens.
7. Em caso de sucesso, o endpoint retorna `201 Created`. Se houver erro de negócio ou produto inexistente, uma exceção faz a transação ser revertida.

### Por que usar bloqueio pessimista?

Um estoque de **uma unidade** não pode confirmar duas compras concorrentes de uma unidade cada. O bloqueio de linha no PostgreSQL faz com que uma transação obtenha o lock antes de modificar o estoque; uma operação concorrente observa a disponibilidade após a liberação do bloqueio. Ordenar os produtos por ID ajuda a **reduzir**, mas não elimina em todos os cenários, o risco de deadlocks.

O teste de concorrência implementado inicia duas requisições com threads e `CountDownLatch`, verificando:

- uma requisição com `201 Created` e outra com `409 Conflict`;
- estoque final igual a zero;
- apenas um novo pedido persistido.

Esse teste foi executado com PostgreSQL real via Testcontainers. Ele é uma evidência de consistência para o cenário definido, **não um teste de estresse nem uma prova universal de ausência de condições de corrida**.

## 5. Contratos HTTP e tratamento de erros

Os Controllers expõem cinco endpoints versionados em `/api/v1`:

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `/products` | Cadastrar produto |
| `GET` | `/products` | Listar produtos |
| `GET` | `/products/{id}` | Consultar produto |
| `POST` | `/orders` | Criar pedido |
| `GET` | `/orders/{id}` | Consultar pedido |

A validação da entrada combina Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Min`, `@Size` etc.) com regras de negócio do serviço. O `ApiExceptionHandler`, baseado em `@RestControllerAdvice`, retorna `ProblemDetail` para:

- **`400 Bad Request`** — violações de validação de parâmetros do corpo tratadas por `MethodArgumentNotValidException`;
- **`404 Not Found`** — produto ou pedido inexistente;
- **`409 Conflict`** — estoque insuficiente, conflitos de negócio ou violações de integridade contempladas pelo handler.

O tratamento explícito de outras entradas malformadas (como JSON inválido) ainda pode ser ampliado. Os nomes dos esquemas OpenAPI são diferenciados para evitar colisões entre DTOs internos `Create` e `View`.

A documentação interativa está disponível, **quando a aplicação está em execução local**, em:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificação OpenAPI: `http://localhost:8080/v3/api-docs`

## 6. Testes e integração contínua

A suíte atual possui **11 testes automatizados**:

- **5 unitários:** comportamento da entidade `Product` e das regras de `OrderService`.
- **6 de integração:** criação do pedido e redução de estoque; estoque insuficiente; rollback quando um produto não existe; quantidade zero; SKU duplicado; duas compras concorrentes.

Os testes de integração usam `@SpringBootTest`, Spring MockMvc, Testcontainers e uma instância temporária de `postgres:17-alpine`. Eles exercitam a aplicação com transações reais de banco. Para executá-los localmente, o Docker Desktop precisa estar acessível.

```bash
mvn clean test
```

O workflow `.github/workflows/ci.yml` utiliza **Java 21** e executa `mvn -B -ntp test` em pushes para `main` e em pull requests. A execução associada ao commit `08a9096` (OpenAPI) foi concluída com sucesso. As Actions utilizadas no workflow ainda precisam de revisão de versões antes da release.

## 7. Execução local e contêineres

- `application.yml`: porta `8080`; conexão padrão com PostgreSQL em `localhost:5433`, configurável por `DB_URL`, `DB_USER` e `DB_PASSWORD`.
- `compose.yaml`: serviço `db` com `postgres:17-alpine`, healthcheck e volume nomeado persistente; porta publicada apenas em `127.0.0.1:5433` no host.
- `compose.yaml`: serviço opcional `api`, habilitado pelo perfil `api`, que usa o hostname `db` na conexão JDBC e depende do banco saudável.
- `Dockerfile`: build em múltiplos estágios (Maven + Java 21; runtime Java 21 JRE) e execução com usuário não privilegiado.

O build e a execução completos da **imagem da API** pelo Docker Compose ainda necessitam de validação específica antes da release. As credenciais presentes no Compose são **exclusivamente de desenvolvimento**. Não se deve usar essas configurações como referência de segurança de produção.

## 8. Decisões técnicas e limites conhecidos

### Decisões

- `BigDecimal` para valores monetários, evitando cálculos com ponto flutuante binário.
- Snapshot do preço e dos dados do produto no item do pedido.
- `@Transactional` + bloqueio pessimista para manter reserva de estoque e criação de pedido consistentes.
- Flyway como fonte de versionamento do esquema e Hibernate com validação do modelo.
- Organização por domínio para manter Controllers, Services, DTOs e Repositories relacionados próximos.
- OpenAPI/Swagger UI como documentação dos contratos HTTP.

### Limitações e trabalho futuro

- **Sem autenticação e autorização:** a API é destinada a execução local; não deve ser exposta publicamente sem medidas adicionais.
- **Sem cancelamento, idempotência ou histórico de movimentações de estoque.**
- **Sem paginação ou filtros avançados** para listagem de produtos.
- **Sem ensaios de carga extensivos**; o teste concorrente atual cobre um caso controlado.
- **Dependência de testes com risco residual:** `net.minidev:json-smart:2.6.0`, identificada no alerta CVE-2026-113602; a avaliação ainda não confirmou uma atualização corrigida adequada. O risco permanece documentado, sem alegação de resolução.
- **Validação pendente antes da release:** build da API via Docker Compose, revisão de versões do workflow CI e harmonização da versão estável com o nome do artefato usado no `Dockerfile`.

Consulte [README](../README.md) e [roadmap](roadmap.md) para instruções de uso e próximas entregas.
