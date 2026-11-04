package dev.vinicius.orderflow.order;

import dev.vinicius.orderflow.product.Product;
import dev.vinicius.orderflow.product.ProductRepository;
import dev.vinicius.orderflow.shared.BusinessConflictException;
import dev.vinicius.orderflow.shared.ResourceNotFoundException;
import java.util.Comparator;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    @Transactional
    public OrderDto.View create(OrderDto.Create input) {
        var uniqueIds = new HashSet<Long>();
        for (var line : input.items()) {
            if (!uniqueIds.add(line.productId())) {
                throw new BusinessConflictException("Produto duplicado no pedido");
            }
        }
        var order = new CustomerOrder(input.customerName().trim());
        // Lock em ordem estável reduz chances de deadlock com pedidos concorrentes.
        var sortedLines = input.items().stream()
                .sorted(Comparator.comparing(OrderDto.Line::productId)).toList();
        for (var line : sortedLines) {
            Product product = products.findByIdForUpdate(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto não encontrado: " + line.productId()));
            product.reserveStock(line.quantity());
            order.addItem(new OrderItem(product, line.quantity()));
        }
        return OrderDto.View.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    public OrderDto.View get(Long id) {
        return OrderDto.View.from(orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado")));
    }
}
