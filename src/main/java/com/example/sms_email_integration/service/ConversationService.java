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

    public Conversation saveConversation(
            String phoneNumber,
            String toNumber,
            String email,
            String message,
            String direction,
            String channel,
            String subject,
            String caseType,    
            String threadId,
            String externalMessageId,
            ConversationThread useConversationThread
    ) {
      
    //   Optional<Conversation> existingConversation = conversationRepository.findByMessageId(externalMessageId);

    //      if (existingConversation.isPresent()) {
    //     System.out.println("Duplicate messageId detected, skipping insert: "  + externalMessageId);
    //     return existingConversation.get(); // Return the existing conversation to avoid re-insert
    // }

        System.out.println("Creating new conversationThread " +phoneNumber +" ," +email +" ," +caseType +" ," + toNumber);

        ConversationThread conversationThread;
        if(useConversationThread == null){
            conversationThread
                = createOrFindThread(phoneNumber, email, caseType, toNumber);
        }else {
            conversationThread = useConversationThread;
        }

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
     * Helper method to find or create a thread based on phoneNumber+email (or
     * the given threadId).
     */
    public ConversationThread createOrFindThread(String phone, String email, String caseType, String toNumber) {
        if (caseType == null || caseType.isEmpty()) {
            caseType = "Unknown";
        }
        // String safeCaseType = caseType.replaceAll("\\s+", "_");
        String generatedThreadId = phone + "-" + email;

        Optional<ConversationThread> existing = conversationThreadRepository.findActiveThreadByThreadId(generatedThreadId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ConversationThread newThread = new ConversationThread();
        newThread.setThreadId(generatedThreadId);
        newThread.setPhoneNumber(phone);
        newThread.setEmail(email);
        newThread.setToNumber(toNumber);
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        newThread.setCaseType(caseType);
        return conversationThreadRepository.save(newThread);
    }

    public List<ConversationDto> getAllConversationsDto() {
        List<Conversation> all = conversationRepository.findAll();
        return all.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

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
            dto.setStatus(entity.getConversationThread().getStatus());
        }

        // Look up whether this phoneNumber has a lawyer assigned
        // 1) Retrieve the phone, firmId, and caseType from the ConversationThread
        String phone = dto.getPhoneNumber();
        Long firmId = dto.getConversationThreadId() != null
                ? conversationThreadRepository.findById(dto.getConversationThreadId())
                        .map(ConversationThread::getCustiId)
                        .orElse(null)
                : null;

// Or, if you have the caseType in your Dto:
        String caseType = entity.getConversationThread().getCaseType();

// 2) If any is null, no firm mapping => skip
        if (phone == null || firmId == null || caseType == null) {
            // we can't do a perfect lookup => no assigned lawyer
            dto.setAssignedLawyerId(null);
            dto.setAssignedLawyerName(null);
            return dto;
        }

// 3) Call the new query that returns exactly one row
        Optional<FirmClientMapping> mappingOpt
                = firmClientMappingRepository.findByPhoneFirmCaseType(phone, firmId, caseType);

        if (mappingOpt.isPresent()) {
            FirmClientMapping mapping = mappingOpt.get();
            if (mapping.getFirmLawyer() != null) {
                dto.setAssignedLawyerId(mapping.getFirmLawyer().getLawyerId());
                dto.setAssignedLawyerName(mapping.getFirmLawyer().getLawyerName());
            }
        } else {
            // no row => no assigned lawyer
            dto.setAssignedLawyerId(null);
            dto.setAssignedLawyerName(null);
        }

        return dto;
    }
}
