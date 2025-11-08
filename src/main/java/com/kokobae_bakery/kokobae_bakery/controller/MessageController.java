package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.dto.MessageDto;
import com.kokobae_bakery.kokobae_bakery.model.Message;
import com.kokobae_bakery.kokobae_bakery.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<Message> createMessage(@RequestBody MessageDto messageDto) {
        Message newMessage = messageService.createMessage(messageDto);
        return ResponseEntity.ok(newMessage);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Message> getAllMessages() {
        return messageService.getAllMessages();
    }

    @PatchMapping("/admin/{id}/{read}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Message> markMessageAsRead(@PathVariable String id, @PathVariable boolean read) {
        return messageService.markMessageAsRead(id, read)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMessage(@PathVariable String id) {
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}
