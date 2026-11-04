package dev.vinicius.orderflow.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ProductDto {
    private ProductDto() {}

    public record Create(
        @NotBlank @Size(max = 48) String sku,
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) int stock
    ) {}

    public record View(Long id, String sku, String name, BigDecimal price, int stock) {
        public static View from(Product product) {
            return new View(product.getId(), product.getSku(), product.getName(),
                    product.getPrice(), product.getStock());
        }
    }
}
