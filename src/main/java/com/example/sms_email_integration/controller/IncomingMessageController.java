package com.example.sms_email_integration.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.IncomingMessageDto;
import com.example.sms_email_integration.service.IncomingMessageService;

@RestController
@RequestMapping("/api/messages")
public class IncomingMessageController {

    private final IncomingMessageService incomingMessageService;

    public IncomingMessageController(IncomingMessageService incomingMessageService) {
        this.incomingMessageService = incomingMessageService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<IncomingMessageDto> createMessage(@RequestBody IncomingMessageDto messageDto) {
        IncomingMessageDto created = incomingMessageService.createMessage(messageDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // READ - Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<IncomingMessageDto> getMessageById(@PathVariable Long id) {
        IncomingMessageDto dto = incomingMessageService.getMessageById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    // READ - Get all
    @GetMapping
    public ResponseEntity<List<IncomingMessageDto>> getAllMessages() {
        List<IncomingMessageDto> all = incomingMessageService.getAllMessages();
        return ResponseEntity.ok(all);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<IncomingMessageDto> updateMessage(@PathVariable Long id,
                                                           @RequestBody IncomingMessageDto updatedDto) {
        IncomingMessageDto updated = incomingMessageService.updateMessage(id, updatedDto);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        boolean deleted = incomingMessageService.deleteMessage(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    
}
