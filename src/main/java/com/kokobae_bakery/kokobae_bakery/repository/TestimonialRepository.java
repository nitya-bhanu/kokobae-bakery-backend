package com.kokobae_bakery.kokobae_bakery.repository;

import com.kokobae_bakery.kokobae_bakery.model.Testimonial;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestimonialRepository extends MongoRepository<Testimonial, String> {
    List<Testimonial> findByVisibleTrueOrderByDisplayOrderAsc();
}
