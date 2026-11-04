package dev.vinicius.orderflow;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "OrderFlow API",
                version = "0.1.0",
                description = """
            API REST para gerenciamento de produtos,
            pedidos e controle de estoque.

            Desenvolvida com Java 21, Spring Boot 4,
            PostgreSQL e Spring Data JPA.

            Inclui validações, transações, controle de
            concorrência e testes automatizados.
            """
        )
)
@SpringBootApplication
public class OrderFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderFlowApplication.class, args);
    }
}
