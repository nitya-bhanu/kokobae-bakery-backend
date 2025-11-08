package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.dto.MessageDto;
import com.kokobae_bakery.kokobae_bakery.model.Message;
import com.kokobae_bakery.kokobae_bakery.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message createMessage(MessageDto messageDto) {
        Message message = new Message();
        message.setName(messageDto.getName());
        message.setEmail(messageDto.getEmail());
        message.setMessage(messageDto.getMessage());
        message.setTimestamp(Instant.now());
        message.setRead(false);
        return messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public Optional<Message> markMessageAsRead(String id, boolean read) {
        return messageRepository.findById(id).map(message -> {
            message.setRead(read);
            return messageRepository.save(message);
        });
    }

    public void deleteMessage(String id) {
        messageRepository.deleteById(id);
    }
}
