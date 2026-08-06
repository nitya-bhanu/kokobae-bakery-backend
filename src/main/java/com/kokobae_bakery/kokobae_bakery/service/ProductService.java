package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Product;
import com.kokobae_bakery.kokobae_bakery.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));
        product.setName(productDetails.getName());
        product.setPrice(productDetails.getPrice());
        product.setImage(productDetails.getImage());
        product.setImages(productDetails.getImages());
        product.setDealSeller(productDetails.getDealSeller());
        product.setOutOfStock(productDetails.getOutOfStock());
        product.setCategory(productDetails.getCategory());
        product.setBestSeller(productDetails.getBestSeller());
        product.setVisible(productDetails.getVisible());
        product.setDescription(productDetails.getDescription());
        product.setImgAltText(productDetails.getImgAltText());
        product.setDiscount(productDetails.getDiscount());
        product.setContainsEgg(productDetails.getContainsEgg());
        product.setIsPriceVisible(productDetails.getIsPriceVisible());
        return productRepository.save(product);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
}
