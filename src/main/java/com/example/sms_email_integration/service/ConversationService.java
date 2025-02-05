package com.example.sms_email_integration.service;

import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.FirmLawyerRepository;


@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    /**
     * Save a new conversation message (SMS or Email).
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
        // Rename the locally generated threadId to avoid clash with the parameter
        String generatedThreadId = buildThreadId(phoneNumber, email);

        String fullUuid = UUID.randomUUID().toString();
        String shortId = fullUuid.substring(0, 5);

        Conversation conversation = new Conversation(
                phoneNumber,
                toNumber, 
                email,
                message,
                direction,
                channel,
                subject,
                LocalDateTime.now(),    // current time
                generatedThreadId,
                externalMessageId
        );
        return conversationRepository.save(conversation);
    }

    /**
     * Fetch all messages by thread_id to get the complete conversation.
     */
    public List<Conversation> getConversationByThreadId(String threadId) {
        return conversationRepository.findByThreadId(threadId);
    }

    /**
     * Fetch all conversations.
     */
    public List<Conversation> getAllConversations() {
        List<Conversation> conversationList = conversationRepository.findAll();
        for (Conversation conversation : conversationList) {
            Optional<FirmLawyer> firmLawyerOptional = firmLawyerRepository.getLawyerByEmail(conversation.getEmail());
            if(firmLawyerOptional.isPresent()) {
                conversation.setFirmLawyer(firmLawyerOptional.get());
            }
        }
        return conversationList;
    }

    /**
     * Fetch a conversation by ID.
     */
    public Optional<Conversation> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    /**
     * Delete a conversation by ID.
     */
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    /**
     * Update an existing conversation message.
     */
    public Conversation updateConversation(Long id, String newMessage) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(id);
        if (optionalConversation.isPresent()) {
            Conversation conversation = optionalConversation.get();
            conversation.setMessage(newMessage);
            conversation.setTimestamp(LocalDateTime.now()); // Update timestamp
            return conversationRepository.save(conversation);
        } else {
            throw new RuntimeException("Conversation not found with ID: " + id);
        }
    }

    /**
     * Utility method to build a thread ID from phone number and email.
     */
    private String buildThreadId(String phoneNumber, String email) {
    if (phoneNumber == null) {
        phoneNumber = "unknown Phone Number";
    }
    if (email == null) {
        email = "unknown Email";
    }

    // Check if email contains angle brackets (like "Name <actual@email>")
    // and extract only what is inside < ... >
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*<(.*)>.*");
    java.util.regex.Matcher matcher = pattern.matcher(email);
    if (matcher.matches()) {
        email = matcher.group(1); // capture only what's inside < >
    }

    // Return phoneNumber + " - " + the cleaned-up email
    return phoneNumber + " - " + email;
}

}
