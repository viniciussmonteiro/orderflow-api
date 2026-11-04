package dev.vinicius.orderflow.product;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ProductDto.View> create(@Valid @RequestBody ProductDto.Create input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/v1/products/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public ProductDto.View get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public List<ProductDto.View> list() { return service.list(); }
}
