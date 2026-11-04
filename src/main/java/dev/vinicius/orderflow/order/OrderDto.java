package dev.vinicius.orderflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class OrderDto {
    private OrderDto() {}

    public record Line(@NotNull Long productId, @Min(1) int quantity) {}

    public record Create(
        @NotBlank @Size(max = 120) String customerName,
        @NotEmpty List<@NotNull @Valid Line> items
    ) {}

    public record ItemView(Long productId, String sku, String name,
                           BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
        static ItemView from(OrderItem item) {
            return new ItemView(item.getProductId(), item.getSku(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
        }
    }

    public record View(Long id, String customerName, String status, BigDecimal total,
                       OffsetDateTime createdAt, List<ItemView> items) {
        public static View from(CustomerOrder order) {
            return new View(order.getId(), order.getCustomerName(), order.getStatus().name(),
                    order.getTotal(), order.getCreatedAt(), order.getItems().stream()
                            .map(ItemView::from).toList());
        }
    }
}
