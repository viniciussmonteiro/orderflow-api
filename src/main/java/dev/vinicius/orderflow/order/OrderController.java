package dev.vinicius.orderflow.order;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderDto.View> create(@Valid @RequestBody OrderDto.Create input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public OrderDto.View get(@PathVariable Long id) { return service.get(id); }
}
