package com.example.sms_email_integration.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.ConversationDto;
import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.service.ConversationService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationThreadRepository conversationThreadRepository;

    @Autowired
    private FirmClientMappingRepository firmClientMappingRepository;

    /**
     * Get all messages in a thread by the threadId from the ConversationThread.
     */
    // @GetMapping("/{threadId}")
    // public ResponseEntity<List<Conversation>> getConversation(@PathVariable String threadId) {
    //     List<Conversation> conversationList = conversationService.getConversationsByThreadId(threadId);
    //     return ResponseEntity.ok(conversationList);
    // }
    @GetMapping("/{threadId}")
    public ResponseEntity<List<ConversationDto>> getConversation(@PathVariable String threadId) {
        // Use the new Dto method:
        List<ConversationDto> conversationList
                = conversationService.getConversationsByThreadIdDto(threadId);
        return ResponseEntity.ok(conversationList);
    }

    /**
     * Retrieve all conversations.
     */
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getAllConversations() {
        // Instead of returning the entity list, return the Dto list:
        List<ConversationDto> allDtos = conversationService.getAllConversationsDto();
        return ResponseEntity.ok(allDtos);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ConversationDto> getConversationById(@PathVariable Long id) {
        Optional<ConversationDto> optionalDto = conversationService.getConversationDtoById(id);
        return optionalDto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Save a new conversation message. Here, you'll need to pass phoneNumber,
     * email, etc. in your request body or some other mechanism so that the
     * service can handle the new normalized logic.
     */
    @PostMapping
    public ResponseEntity<Conversation> saveNewConversation(@RequestBody Conversation conversation) {
        // In your new approach, "toNumber" is no longer on the Conversation entity.
        // Instead, you might pull it from the ConversationThread or from a custom DTO.

        String phoneNumber = conversation.getConversationThread().getPhoneNumber();
        String toNumber = conversation.getConversationThread().getToNumber(); // Moved to thread
        String email = conversation.getConversationThread().getEmail();
        String threadId = conversation.getConversationThread().getThreadId();

        Conversation saved = conversationService.saveConversation(
                phoneNumber,
                toNumber, // pass it here
                email,
                conversation.getMessage(),
                conversation.getDirection(),
                conversation.getChannel(),
                conversation.getSubject(),
                null,
                threadId,
                conversation.getMessageId(),
                null
        );

        return ResponseEntity.ok(saved);
    }

    /**
     * Update an existing conversation message.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Conversation> updateConversation(@PathVariable Long id, @RequestBody String newMessage) {
        return ResponseEntity.ok(conversationService.updateConversation(id, newMessage));
    }

    @PutMapping("/thread/{threadId}/resolve")
    public ResponseEntity<String> resolveThread(@PathVariable Long threadId) {
        Optional<ConversationThread> optional = conversationThreadRepository.findById(threadId);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConversationThread thread = optional.get();
        thread.setStatus("RESOLVED");
        conversationThreadRepository.save(thread);

        return ResponseEntity.ok("Thread " + threadId + " marked as RESOLVED");
    }

    @PutMapping("/thread/{threadId}/caseType")
    public ResponseEntity<String> updateThreadCaseType(
            @PathVariable Long threadId,
            @RequestBody Map<String, String> requestBody
    ) {
        String newCaseType = requestBody.get("caseType");
        if (newCaseType == null || newCaseType.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing or empty caseType");
        }

        // 1) Find the thread
        Optional<ConversationThread> optionalThread = conversationThreadRepository.findById(threadId);
        if (optionalThread.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ConversationThread thread = optionalThread.get();

        // 2) Save oldCaseType, then update to new
        String oldCaseType = thread.getCaseType();    // <-- store old
        thread.setCaseType(newCaseType);              // <-- set new
        conversationThreadRepository.save(thread);

        // 3) Update firm_client_mapping row by oldCaseType
        String phone = thread.getPhoneNumber();
        Long firmId = thread.getCustiId();
        if (phone != null && firmId != null) {
            // Find the row with the *old* case type
            Optional<FirmClientMapping> existingMappingOpt
                    = firmClientMappingRepository.findByPhoneFirmCaseType(phone, firmId, oldCaseType);

            if (existingMappingOpt.isPresent()) {
                FirmClientMapping mapping = existingMappingOpt.get();
                mapping.setCaseType(newCaseType);         // <--- update to new
                firmClientMappingRepository.save(mapping);
            } else {
                // If you want, create a new row if none was found,
                // or just ignore if no old row existed.
            }
        }
        return ResponseEntity.ok("CaseType updated from " + oldCaseType + " → " + newCaseType);
    }

    /**
     * Delete a conversation by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/firm/{firmId}")
    public ResponseEntity<List<ConversationDto>> getConversationsByFirm(@PathVariable Long firmId) {
        List<ConversationDto> conversations = conversationService.getConversationsByFirmId(firmId);
        return ResponseEntity.ok(conversations);
    }

     @GetMapping("/descending")
    public ResponseEntity<List<ConversationDto>> getAllConversationsDescending() {
        List<ConversationDto> dtos = conversationService.getAllConversationsDescending();
        return ResponseEntity.ok(dtos);
    }

    
    @GetMapping("/channel/{channel}/descending")
    public ResponseEntity<List<ConversationDto>> getAllConversationsByChannelDescending(
            @PathVariable String channel
    ) {
        List<ConversationDto> dtos =
            conversationService.getAllConversationsByChannelDescending(channel.toUpperCase());
        return ResponseEntity.ok(dtos);
    }

   
    @GetMapping("/direction/{direction}/descending")
    public ResponseEntity<List<ConversationDto>> getAllConversationsByDirectionDescending(
            @PathVariable String direction
    ) {
        List<ConversationDto> dtos =
            conversationService.getAllConversationsByDirectionDescending(direction.toUpperCase());
        return ResponseEntity.ok(dtos);
    }



}
