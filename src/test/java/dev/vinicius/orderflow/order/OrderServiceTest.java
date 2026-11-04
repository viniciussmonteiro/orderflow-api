package dev.vinicius.orderflow.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.vinicius.orderflow.product.Product;
import dev.vinicius.orderflow.product.ProductRepository;
import dev.vinicius.orderflow.shared.BusinessConflictException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @InjectMocks OrderService service;

    private Product product;

    @BeforeEach
    void setup() throws Exception {
        product = new Product("SKU-1", "Livro", new BigDecimal("19.90"), 5);
        Field id = Product.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(product, 1L);
    }

    @Test
    void createsOrderAndReservesStock() {
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(orders.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var input = new OrderDto.Create("Cliente", List.of(new OrderDto.Line(1L, 2)));

        var result = service.create(input);

        assertEquals(new BigDecimal("39.80"), result.total());
        assertEquals(3, product.getStock());
        assertEquals(1, result.items().size());
    }

    @Test
    void preventsOrderWhenOutOfStock() {
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        var input = new OrderDto.Create("Cliente", List.of(new OrderDto.Line(1L, 6)));

        assertThrows(BusinessConflictException.class, () -> service.create(input));
        verify(orders, never()).save(any(CustomerOrder.class));
        assertEquals(5, product.getStock());
    }

    @Test
    void rejectsDuplicatedProductLines() {
        var input = new OrderDto.Create("Cliente", List.of(
                new OrderDto.Line(1L, 1), new OrderDto.Line(1L, 2)));
        assertThrows(BusinessConflictException.class, () -> service.create(input));
        verify(orders, never()).save(any(CustomerOrder.class));
    }
}
