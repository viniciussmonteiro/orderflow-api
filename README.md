# OrderFlow API

API REST de **pedidos e estoque** criada para estudo de backend Java em um contexto próximo de aplicações comerciais.

> **Status: MVP inicial / em desenvolvimento.** Código-fonte gerado como ponto de partida. Build e testes devem ser executados no seu computador antes de publicar como projeto validado. Ainda não tem autenticação e não deve ser exposta diretamente à internet.

## Stack

Java 21 · Spring Boot 4.1.2 · Spring Web MVC · Spring Data JPA · PostgreSQL · Flyway · Bean Validation · JUnit 5 · Mockito · Docker Compose · GitHub Actions

## Escopo atual

- Cadastro e consulta de produtos (`sku`, `name`, `price`, `stock`).
- Criação e consulta de pedidos, incluindo itens e total com `BigDecimal`.
- Verificação de estoque disponível e reserva no mesmo escopo transacional.
- Lock pessimista por produto, em ordem consistente, para mitigar compras concorrentes.
- Migração versionada do esquema PostgreSQL por Flyway.
- Respostas com códigos HTTP adequados para validação, recurso inexistente e conflitos de negócio.
- Testes unitários iniciais da reserva de estoque e da criação de pedidos.

## Como executar

Requisitos: **JDK 21, Maven 3.9+ e Docker com Docker Compose**.

1. Inicie o banco:

   ```bash
   docker compose up -d db
   ```

2. Suba a API pelo Maven:

   ```bash
   mvn spring-boot:run
   ```

3. Em outro terminal, teste as rotas. No Windows PowerShell, use `curl.exe` para garantir a sintaxe mostrada.

   ```bash
   curl -i http://localhost:8080/api/v1/products
   curl -i -X POST http://localhost:8080/api/v1/products -H "Content-Type: application/json" -d '{"sku":"BOOK-001","name":"Livro Java","price":59.90,"stock":10}'
   curl -i -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -d '{"customerName":"Ana","items":[{"productId":1,"quantity":2}]}'
   curl -i http://localhost:8080/api/v1/orders/1
   ```

   Os comandos POST acima usam sintaxe de shell tipo Bash; para PowerShell, use um arquivo JSON com `curl.exe --data-binary @pedido.json` ou `Invoke-RestMethod`.

Opcional: execute a API também via Docker:

```bash
docker compose --profile api up --build
```

> Credenciais do `compose.yaml` são apenas para desenvolvimento local. Para produção, utilize secrets/variáveis de ambiente seguras, HTTPS e autenticação.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/products` | Cria produto |
| `GET` | `/api/v1/products` | Lista produtos |
| `GET` | `/api/v1/products/{id}` | Busca produto |
| `POST` | `/api/v1/orders` | Cria pedido |
| `GET` | `/api/v1/orders/{id}` | Consulta pedido |

## Testes

```bash
mvn test
```

Os testes unitários atuais usam Mockito e não iniciam PostgreSQL. **Próxima etapa:** adicionar Testcontainers e testes de integração/concorrência reais, além de rodar e corrigir a pipeline CI.

## Estrutura

```text
src/main/java/dev/vinicius/orderflow/
├── product/  # entidade, DTO, controller, service e repository
├── order/    # criação de pedidos e itens
└── shared/   # exceções e tratamento HTTP
src/main/resources/db/migration/ # SQL Flyway
docs/                             # arquitetura e roadmap
.github/workflows/                # CI
```

## Próximas entregas

Veja [`docs/roadmap.md`](docs/roadmap.md). Prioridades: testes de integração com PostgreSQL real, documentação Swagger, autenticação, cancelamento de pedidos e observabilidade.

## Autor

Projeto de portfólio de [Vinicius Monteiro](https://github.com/viniciussmonteiro).
