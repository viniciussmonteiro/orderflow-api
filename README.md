# OrderFlow API

API REST para gerenciamento de **produtos, pedidos e estoque**, desenvolvida em Java e Spring Boot para explorar regras de negócio comuns em sistemas comerciais.

O projeto concentra-se na consistência dos dados: ao confirmar um pedido, a aplicação valida a disponibilidade dos produtos, calcula os valores e atualiza o estoque dentro de uma transação.

**Status:** MVP inicial (`0.1.0-SNAPSHOT`) — em desenvolvimento.

## Tecnologias

- **Java 21** e **Spring Boot 4.1.2** (Spring Web MVC, Spring Data JPA e Bean Validation)
- **PostgreSQL 17**, **Hibernate** e **Flyway**
- **JUnit 5** e **Mockito**
- **Maven**, **Docker Compose** e **GitHub Actions** (workflow configurado; validação da execução pendente)

## Funcionalidades implementadas

- Cadastro, listagem e consulta de produtos por ID.
- Criação e consulta de pedidos com múltiplos itens.
- Cálculo de subtotais e total com `BigDecimal`.
- Verificação de estoque e redução da quantidade disponível após a confirmação do pedido.
- Tratamento de erros HTTP para dados inválidos, recursos inexistentes e conflitos de negócio.
- Persistência com JPA e controle de esquema do banco com migrações Flyway.
- Uso de transação e bloqueio pessimista na criação de pedidos; testes de concorrência ainda estão planejados.

## Endpoints

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/products` | Cadastrar produto |
| `GET` | `/api/v1/products` | Listar produtos |
| `GET` | `/api/v1/products/{id}` | Consultar produto |
| `POST` | `/api/v1/orders` | Criar pedido |
| `GET` | `/api/v1/orders/{id}` | Consultar pedido |

## Executando localmente

**Pré-requisitos:** JDK 21, Maven 3.9+ e Docker com Docker Compose.

1. Inicie o PostgreSQL:

   ```bash
   docker compose up -d db
   ```

2. Inicie a aplicação:

   ```bash
   mvn spring-boot:run
   ```

3. Consulte os produtos em `http://localhost:8080/api/v1/products`.

A configuração local usa **porta 8080** para a API e **porta 5433** no host para o PostgreSQL (que continua utilizando a porta 5432 dentro do contêiner). As credenciais do Compose são exclusivas do ambiente de desenvolvimento.

### Exemplo de uso no Windows PowerShell

Cadastrar um produto:

```powershell
$produto = @{
    sku = 'BOOK-001'
    name = 'Livro Java'
    price = 59.90
    stock = 10
} | ConvertTo-Json

$novoProduto = Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/products' `
    -ContentType 'application/json' -Body $produto

$novoProduto
```

Criar um pedido usando o ID retornado no cadastro:

```powershell
$pedido = @{
    customerName = 'Cliente Teste'
    items = @(@{ productId = $novoProduto.id; quantity = 2 })
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
    -Uri 'http://localhost:8080/api/v1/orders' `
    -ContentType 'application/json' -Body $pedido
```

Consulte `/api/v1/products` novamente para verificar a quantidade em estoque. Use outro SKU caso já tenha cadastrado `BOOK-001`.

## Testes e validações

Os **5 testes unitários existentes foram executados com sucesso** em ambiente local (`mvn test`), sem falhas. Eles cobrem comportamentos de produto e serviço de pedidos, mas não substituem testes de integração com PostgreSQL.

Também foram validados manualmente os seguintes fluxos com a API e o banco em execução:

- Cadastro de um produto com 10 unidades em estoque.
- Criação de pedido com 2 unidades, totalizando R$ 119,80.
- Redução de estoque de 10 para 8 unidades.
- Rejeição de um pedido de 9 unidades quando havia apenas 8 em estoque: a API retornou `409 Conflict` e preservou as 8 unidades.

Para executar os testes:

```bash
mvn test
```

## Organização do projeto

```text
src/main/java/dev/vinicius/orderflow/
├── product/    # Produtos e regras de estoque
├── order/      # Pedidos e itens
└── shared/     # Exceções e tratamento de erros
src/main/resources/db/migration/  # Migrações Flyway
src/test/                        # Testes automatizados
docs/                            # Arquitetura e planejamento
.github/workflows/               # Configuração de CI
```

## Próximos passos

- Testes de integração com Testcontainers e cenários de concorrência.
- Revisão das dependências e alertas de segurança.
- Documentação OpenAPI/Swagger UI.
- Evolução do catálogo, cancelamento de pedidos e autenticação.

O planejamento está documentado em [`docs/roadmap.md`](docs/roadmap.md).

> **Limitação atual:** a API não possui autenticação. O projeto foi configurado para execução local e não deve ser exposto diretamente à internet como serviço de produção.

## Autor

**[Vinicius Monteiro](https://github.com/viniciussmonteiro)** — projeto de portfólio Java Backend.
