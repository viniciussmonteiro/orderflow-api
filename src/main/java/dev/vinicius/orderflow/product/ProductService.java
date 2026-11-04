package dev.vinicius.orderflow.product;

import dev.vinicius.orderflow.shared.BusinessConflictException;
import dev.vinicius.orderflow.shared.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    @Transactional
    public ProductDto.View create(ProductDto.Create input) {
        String sku = input.sku().trim();
        if (repository.existsBySku(sku)) {
            throw new BusinessConflictException("SKU já cadastrado");
        }
        Product product = repository.save(new Product(sku, input.name().trim(),
                input.price(), input.stock()));
        return ProductDto.View.from(product);
    }

    @Transactional(readOnly = true)
    public ProductDto.View get(Long id) {
        return ProductDto.View.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado")));
    }

    @Transactional(readOnly = true)
    public List<ProductDto.View> list() {
        return repository.findAll(Sort.by("id").ascending()).stream().map(ProductDto.View::from).toList();
    }
}
