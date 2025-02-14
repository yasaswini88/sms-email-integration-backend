package com.example.sms_email_integration.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.sms_email_integration.dto.IncomingMessageDto;
import com.example.sms_email_integration.entity.IncomingMessage;
import com.example.sms_email_integration.repository.IncomingMessageRepository;

@Service
public class IncomingMessageService {

    private final IncomingMessageRepository incomingMessageRepository;

    public IncomingMessageService(IncomingMessageRepository incomingMessageRepository) {
        this.incomingMessageRepository = incomingMessageRepository;
    }

    // CREATE
    public IncomingMessageDto createMessage(IncomingMessageDto messageDto) {
        IncomingMessage entity = new IncomingMessage(
                messageDto.getFromNumber(),
                messageDto.getToNumber(),
                messageDto.getBody(),
                messageDto.getReceivedAt()
        );


        entity.setDirection(messageDto.getDirection()); 

        IncomingMessage saved = incomingMessageRepository.save(entity);
        return entityToDto(saved);
    }

    // READ by ID
    public IncomingMessageDto getMessageById(Long id) {
        Optional<IncomingMessage> optional = incomingMessageRepository.findById(id);
        return optional.map(this::entityToDto).orElse(null);
    }

    // READ all
    public List<IncomingMessageDto> getAllMessages() {
        List<IncomingMessage> messages = incomingMessageRepository.findAll();
        return messages.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // UPDATE
    public IncomingMessageDto updateMessage(Long id, IncomingMessageDto updatedDto) {
        Optional<IncomingMessage> optional = incomingMessageRepository.findById(id);
        if (optional.isEmpty()) {
            return null; // or throw an exception
        }
        IncomingMessage existing = optional.get();
        existing.setFromNumber(updatedDto.getFromNumber());
        existing.setToNumber(updatedDto.getToNumber());
        existing.setBody(updatedDto.getBody());
        existing.setReceivedAt(updatedDto.getReceivedAt());

        IncomingMessage saved = incomingMessageRepository.save(existing);
        return entityToDto(saved);
    }

    // DELETE
    public boolean deleteMessage(Long id) {
        Optional<IncomingMessage> optional = incomingMessageRepository.findById(id);
        if (optional.isPresent()) {
            incomingMessageRepository.delete(optional.get());
            return true;
        }
        return false;
    }

    // Helper method: Entity -> DTO
    private IncomingMessageDto entityToDto(IncomingMessage entity) {
        IncomingMessageDto dto = new IncomingMessageDto();
        dto.setMsgId(entity.getMsgId());
        dto.setFromNumber(entity.getFromNumber());
        dto.setToNumber(entity.getToNumber());
        dto.setBody(entity.getBody());
        dto.setReceivedAt(entity.getReceivedAt());
        dto.setDirection(entity.getDirection());
        return dto;
    }


     public List<IncomingMessageDto> getAllMessagesDescending() {
        List<IncomingMessage> messages = incomingMessageRepository.findByOrderByReceivedAtDesc();
        return messages.stream()
                       .map(this::entityToDto)
                       .collect(Collectors.toList());
    }

    
}
