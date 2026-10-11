package com.kokobae_bakery.kokobae_bakery.repository;

import com.kokobae_bakery.kokobae_bakery.model.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends MongoRepository<Tag, String> {
    Optional<Tag> findByName(String name);
}
