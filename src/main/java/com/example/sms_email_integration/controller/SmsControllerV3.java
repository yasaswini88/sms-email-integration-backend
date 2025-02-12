package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.entity.Conversation;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.entity.IncomingMessage;
import com.example.sms_email_integration.repository.ConversationRepository;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.repository.IncomingMessageRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailService;
import com.example.sms_email_integration.service.SmsService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class SmsControllerV3 {

    @Autowired
    private IncomingMessageRepository incomingMessageRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private FirmClientMappingRepository firmClientMappingRepository;
    
    @Autowired
    private FirmLawyerRepository firmLawyerRepository;
    
    @Autowired
    private ConversationThreadRepository conversationThreadRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    /**
     * Receives inbound SMS from Twilio, saves to DB, and forwards to the assigned
     * lawyer (if found), or to the main firm email otherwise.
     */
    @PostMapping(value = "/incoming-sms", consumes = "application/x-www-form-urlencoded")
    public void handleIncomingSms(
            @RequestParam("From") String fromNumber,
            @RequestParam("To") String toNumber,
            @RequestParam("Body") String messageBody,
            @RequestParam("MessageSid") String messageSid,
            HttpServletResponse response
    ) {
        System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

        // 1) Save the raw inbound message.
        IncomingMessage incomingMsg = new IncomingMessage(fromNumber, toNumber, messageBody, LocalDateTime.now());
        incomingMessageRepository.save(incomingMsg);

        // 2) Identify which firm this 'toNumber' belongs to.
        Optional<Customer> optionalCustomer = customerRepository.findByTwilioNumber(toNumber);
        if (optionalCustomer.isEmpty()) {
            System.err.println("No firm (Customer) found for Twilio number: " + toNumber);
            response.setContentType("text/plain");
            return;
        }
        Customer firm = optionalCustomer.get();
        Long firmId = firm.getCusti_id();

        // 3) Check if the client (fromNumber) is mapped to a specific lawyer.
        //    If not found or lawyer is null, we forward to the main firm’s email.
        Optional<FirmClientMapping> clientMappingOpt =
                firmClientMappingRepository.findByClientPhoneNumberAndCustiId(fromNumber, firmId);

        String forwardEmail; 
        if (clientMappingOpt.isPresent() && clientMappingOpt.get().getFirmLawyer() != null) {
            FirmLawyer assignedLawyer = clientMappingOpt.get().getFirmLawyer();
            forwardEmail = assignedLawyer.getLawyerMail();
            System.out.println("Found assigned lawyer: " + assignedLawyer.getLawyerMail());
        } else {
            forwardEmail = firm.getCustMail();  
            System.out.println("No assigned lawyer found; forwarding to firm’s email: " + forwardEmail);
        }

        // 4) Create or find a conversation thread for record-keeping.
        String threadId = fromNumber + "-" + forwardEmail;
        ConversationThread thread = findOrCreateThread(threadId, fromNumber, toNumber, forwardEmail, firmId);

        // 5) Save the inbound SMS in the 'conversation' table.
        Conversation conversation = conversationService.saveConversation(
                fromNumber,
                toNumber,
                forwardEmail,
                messageBody,
                "INCOMING",
                "SMS",
                null,           // subject
                "Unknown",      // you can store a default or blank caseType
                threadId,
                messageSid,
                thread
        );
        System.out.println("Saved new conversation entry. ID=" + conversation.getId());

        // 6) Forward the SMS content (as an email) to the assigned recipient.
        String subject = "SMS from " + fromNumber;
        try {
            emailService.sendEmail(
                    forwardEmail,
                    subject,
                    "Unknown",
                    messageBody,
                    fromNumber,
                    toNumber,
                    messageSid + ": Email",
                    thread
            );
            System.out.println("Successfully forwarded inbound SMS to " + forwardEmail);
        } catch (Exception ex) {
            System.err.println("Error sending email: " + ex.getMessage());
        }
    }

    /**
     * Find or create a ConversationThread for a given (threadId, fromNumber, toNumber, email).
     * This ensures we have a single place to group conversations.
     */
    private ConversationThread findOrCreateThread(
            String threadId,
            String phoneNumber,
            String toNumber,
            String email,
            Long custiId
    ) {
        // Check if it already exists
        Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findByThreadId(threadId);
        if (existingThreadOpt.isPresent()) {
            return existingThreadOpt.get();
        }

        // Otherwise create a new one
        ConversationThread newThread = new ConversationThread();
        newThread.setThreadId(threadId);
        newThread.setPhoneNumber(phoneNumber);
        newThread.setToNumber(toNumber);
        newThread.setEmail(email);
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        newThread.setCaseType("Unknown");
        newThread.setCustiId(custiId);
        return conversationThreadRepository.save(newThread);
    }

}
