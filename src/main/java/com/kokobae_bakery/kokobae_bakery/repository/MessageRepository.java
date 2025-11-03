package com.kokobae_bakery.kokobae_bakery.repository;

import com.kokobae_bakery.kokobae_bakery.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
}
