
package dev.vinicius.orderflow.integration;

import dev.vinicius.orderflow.order.OrderRepository;
import dev.vinicius.orderflow.product.Product;
import dev.vinicius.orderflow.product.ProductRepository;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Test
    void shouldCreateOrderAndUpdateStock() throws Exception {

        Product product = products.saveAndFlush(
                new Product("TEST-ORDER", "Livro Java",
                        new BigDecimal("59.90"), 10)
        );

        String request = """
                {
                  "customerName": "Cliente Teste",
                  "items": [
                    {"productId": %d, "quantity": 2}
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        Product updated = products.findById(product.getId())
                .orElseThrow();

        assertEquals(8, updated.getStock());
    }

    @Test
    void shouldRejectInsufficientStock() throws Exception {

        Product product = products.saveAndFlush(
                new Product("TEST-STOCK", "Produto",
                        new BigDecimal("20.00"), 5)
        );

        String request = """
                {
                  "customerName": "Cliente Teste",
                  "items": [
                    {"productId": %d, "quantity": 6}
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());

        assertEquals(5, products.findById(product.getId())
                .orElseThrow().getStock());
    }

    @Test
    void shouldRollbackWhenProductDoesNotExist() throws Exception {

        Product product = products.saveAndFlush(
                new Product("TEST-ROLLBACK", "Produto",
                        new BigDecimal("30.00"), 8)
        );

        long ordersBefore = orders.count();

        String request = """
                {
                  "customerName": "Teste Rollback",
                  "items": [
                    {"productId": %d, "quantity": 2},
                    {"productId": 999999, "quantity": 1}
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());

        assertEquals(8, products.findById(product.getId())
                .orElseThrow().getStock());

        assertEquals(ordersBefore, orders.count());
    }

    @Test
    void shouldRejectZeroQuantity() throws Exception {

        Product product = products.saveAndFlush(
                new Product("TEST-ZERO", "Produto",
                        new BigDecimal("10.00"), 5)
        );

        String request = """
                {
                  "customerName": "Cliente Teste",
                  "items": [
                    {"productId": %d, "quantity": 0}
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(5, products.findById(product.getId())
                .orElseThrow().getStock());
    }

    @Test
    void shouldRejectDuplicateSku() throws Exception {

        products.saveAndFlush(
                new Product("TEST-DUPLICATE", "Produto Original",
                        new BigDecimal("10.00"), 5)
        );

        String request = """
                {
                  "sku": "TEST-DUPLICATE",
                  "name": "Outro Produto",
                  "price": 20.00,
                  "stock": 5
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }
}
