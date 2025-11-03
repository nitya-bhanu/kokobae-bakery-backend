package com.kokobae_bakery.kokobae_bakery.repository;

import com.kokobae_bakery.kokobae_bakery.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
}
