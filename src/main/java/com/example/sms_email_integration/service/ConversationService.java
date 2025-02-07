package com.example.sms_email_integration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sms_email_integration.dto.ConversationDto;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.repository.ConversationRepository;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationThreadRepository conversationThreadRepository;


    @Autowired
private FirmClientMappingRepository firmClientMappingRepository;

    /**
     * Save a new conversation messfindOrCreateThreadage (SMS or Email).
     * In the normalized version, we either find an existing
     * ConversationThread or create a new one, then associate
     * the Conversation with that thread.
     */
    public Conversation saveConversation(
            String phoneNumber,
            String toNumber,
            String email,
            String message,
            String direction,
            String channel,
            String subject,
            String threadId,
            String externalMessageId
    ) {
        // 1) Find or create a ConversationThread
        ConversationThread conversationThread = new ConversationThread();
        conversationThread.setPhoneNumber(phoneNumber);
        conversationThread.setEmail(email);
        conversationThread.setToNumber(toNumber);
        conversationThread.setThreadId(threadId);
        conversationThread.setCreatedAt(LocalDateTime.now());

        conversationThread = findOrCreateThread(phoneNumber, email, threadId,toNumber);

        // 2) Build a new Conversation referencing that Thread
        Conversation conversation = new Conversation(
                conversationThread,
                message,
                direction,
                channel,
                subject,
                LocalDateTime.now(),
                externalMessageId
                
        );

        return conversationRepository.save(conversation);
    }

    /**
     * Helper method to find or create a thread based on
     * phoneNumber+email (or the given threadId).
     */
    private ConversationThread findOrCreateThread(String phoneNumber, String email, String threadId,String toNumber) {

        // If you already generate a threadId as phoneNumber + " - " + email
        // you can do:
        if (threadId == null || threadId.isEmpty()) {
            threadId = phoneNumber + " - " + email;
        }

        // Attempt to find it
        // Optional<ConversationThread> existing = conversationThreadRepository.findByThreadId(threadId);
        Optional<ConversationThread> existing = conversationThreadRepository.findActiveThreadByThreadId(threadId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Otherwise, create
        ConversationThread newThread = new ConversationThread();
        newThread.setThreadId(threadId);
        newThread.setPhoneNumber(phoneNumber);
        newThread.setEmail(email);
        newThread.setToNumber(toNumber);
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        return conversationThreadRepository.save(newThread);
    }

  public List<ConversationDto> getAllConversationsDto() {
    List<Conversation> all = conversationRepository.findAll();
    return all.stream()
              .map(this::convertToDto)
              .collect(Collectors.toList());
}

    // public List<Conversation> getConversationsByThreadId(String threadId) {
    //     return conversationRepository.findAllByThreadId(threadId);
    // }

    public List<ConversationDto> getConversationsByThreadIdDto(String threadId) {
    List<Conversation> convs = conversationRepository.findAllByThreadId(threadId);
    return convs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
}


public Optional<ConversationDto> getConversationDtoById(Long id) {
    return conversationRepository.findById(id)
            .map(this::convertToDto); // if present
}


    // public Optional<Conversation> getConversationById(Long id) {
    //     return conversationRepository.findById(id);
    // }

    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    public Conversation updateConversation(Long id, String newMessage) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(id);
        if (optionalConversation.isPresent()) {
            Conversation conversation = optionalConversation.get();
            conversation.setMessage(newMessage);
            conversation.setTimestamp(LocalDateTime.now());
            return conversationRepository.save(conversation);
        } else {
            throw new RuntimeException("Conversation not found with ID: " + id);
        }
    }

    private ConversationDto convertToDto(Conversation entity) {
    ConversationDto dto = new ConversationDto();

    // Basic fields from the Conversation entity itself
    dto.setId(entity.getId());
    dto.setMessage(entity.getMessage());
    dto.setDirection(entity.getDirection());
    dto.setChannel(entity.getChannel());
    dto.setSubject(entity.getSubject());
    dto.setTimestamp(entity.getTimestamp());
    dto.setMessageId(entity.getMessageId());

    // Fields pulled from the ConversationThread relationship
    if (entity.getConversationThread() != null) {
        dto.setThreadId(entity.getConversationThread().getThreadId());
        dto.setPhoneNumber(entity.getConversationThread().getPhoneNumber());
        dto.setToNumber(entity.getConversationThread().getToNumber());
        dto.setEmail(entity.getConversationThread().getEmail());
        dto.setConversationThreadId(entity.getConversationThread().getConversationThreadId());
    }

    // Look up whether this phoneNumber has a lawyer assigned
    String phone = dto.getPhoneNumber();  // e.g. "+17038620152"
    if (phone != null) {
        // Attempt to find a FirmClientMapping
        // Adjust to .findByClientPhoneNumberAndCustiId(...) if you store by firm, etc.
        Optional<FirmClientMapping> mappingOpt =
                firmClientMappingRepository.findByClientPhoneNumber(phone);

        if (mappingOpt.isPresent()) {
            // If assigned, fill out the assigned lawyer
            FirmClientMapping mapping = mappingOpt.get();
            if (mapping.getFirmLawyer() != null) {
                dto.setAssignedLawyerId(mapping.getFirmLawyer().getLawyerId());
                dto.setAssignedLawyerName(mapping.getFirmLawyer().getLawyerName());
            }
        } else {
            // No mapping → no assigned lawyer
            dto.setAssignedLawyerId(null);
            dto.setAssignedLawyerName(null);
        }
    }

    return dto;
}

}
