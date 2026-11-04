package dev.vinicius.orderflow.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import dev.vinicius.orderflow.shared.BusinessConflictException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void reservesStockWhenSufficient() {
        var product = new Product("SKU-1", "Mouse", new BigDecimal("59.90"), 5);
        product.reserveStock(2);
        assertEquals(3, product.getStock());
    }

    @Test
    void rejectsInsufficientStock() {
        var product = new Product("SKU-1", "Mouse", new BigDecimal("59.90"), 1);
        assertThrows(BusinessConflictException.class, () -> product.reserveStock(2));
        assertEquals(1, product.getStock());
    }
}
