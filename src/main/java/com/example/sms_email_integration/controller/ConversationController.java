package com.example.sms_email_integration.controller;

import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * Retrieve all messages for a particular thread (identified by threadId).
     */
    @GetMapping("/{threadId}")
    public ResponseEntity<List<Conversation>> getConversation(@PathVariable String threadId) {
        List<Conversation> conversationList = conversationService.getConversationByThreadId(threadId);
        return ResponseEntity.ok(conversationList);
    }

    /**
     * Retrieve all conversations.
     */
    @GetMapping
    public ResponseEntity<List<Conversation>> getAllConversations() {
        return ResponseEntity.ok(conversationService.getAllConversations());
    }

    /**
     * Retrieve a specific conversation by ID.
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<Conversation> getConversationById(@PathVariable Long id) {
        Optional<Conversation> conversation = conversationService.getConversationById(id);
        return conversation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Save a new conversation message.
     */
    @PostMapping
    public ResponseEntity<Conversation> saveConversation(@RequestBody Conversation conversation) {
        return ResponseEntity.ok(conversationService.saveConversation(
                conversation.getPhoneNumber(),
                conversation.getToNumber(),
                conversation.getEmail(),
                conversation.getMessage(),
                conversation.getDirection(),
                conversation.getChannel(),
                conversation.getSubject(),
                conversation.getThreadId(),
                conversation.getMessageId()
        ));
    }

    /**
     * Update an existing conversation message.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Conversation> updateConversation(@PathVariable Long id, @RequestBody String newMessage) {
        return ResponseEntity.ok(conversationService.updateConversation(id, newMessage));
    }

    /**
     * Delete a conversation by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
