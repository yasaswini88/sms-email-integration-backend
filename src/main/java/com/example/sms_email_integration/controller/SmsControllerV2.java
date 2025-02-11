package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.IntakeCheckDto;
import com.example.sms_email_integration.dto.NewCaseCheckDto;
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
import com.example.sms_email_integration.repository.IncomingMessageRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailService;
import com.example.sms_email_integration.service.OpenAiService;
import com.example.sms_email_integration.service.SmsService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v2")
public class SmsControllerV2 {

    private final IncomingMessageRepository incomingMessageRepository;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;
    private final ConversationService conversationService;
    private final FirmClientMappingRepository firmClientMappingRepository;
    private final SmsService smsService;
    private final OpenAiService openAiService;
    private final ConversationThreadRepository conversationThreadRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public SmsControllerV2(
            IncomingMessageRepository incomingMessageRepository,
            EmailService emailService,
            CustomerRepository customerRepository,
            ConversationService conversationService,
            FirmClientMappingRepository firmClientMappingRepository,
            SmsService smsService,
            OpenAiService openAiService,
            ConversationThreadRepository conversationThreadRepository,
            ConversationRepository conversationRepository
    ) {
        this.incomingMessageRepository = incomingMessageRepository;
        this.emailService = emailService;
        this.customerRepository = customerRepository;
        this.conversationService = conversationService;
        this.firmClientMappingRepository = firmClientMappingRepository;
        this.smsService = smsService;
        this.openAiService = openAiService;
        this.conversationThreadRepository = conversationThreadRepository;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping(value = "/incoming-sms", consumes = "application/x-www-form-urlencoded")
    public void handleIncomingSms(
            @RequestParam("From") String fromNumber,
            @RequestParam("To") String toNumber,
            @RequestParam("Body") String messageBody,
            @RequestParam("MessageSid") String messageSid,
            HttpServletResponse response
    ) {
        IncomingMessage incomingMsg = new IncomingMessage(fromNumber, toNumber, messageBody, LocalDateTime.now());
        incomingMessageRepository.save(incomingMsg);
        System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

        Optional<Customer> optionalCustomer = customerRepository.findByTwilioNumber(toNumber);
        if (optionalCustomer.isEmpty()) {
            System.err.println("No customer record found for Twilio number: " + toNumber);
            response.setContentType("text/plain");
            return;
        }

        Customer customer = optionalCustomer.get();
        Long firmId = customer.getCusti_id();

        List<FirmClientMapping> existingNonUnknownOpt = firmClientMappingRepository.findNonUnknownMapping(fromNumber, firmId);
        boolean skipCaseTypeAiCheck = !existingNonUnknownOpt.isEmpty();

        if (skipCaseTypeAiCheck) {
            handleExistingCase(fromNumber, toNumber, messageBody, messageSid, customer, firmId, existingNonUnknownOpt);
        } else {
            handleNewCase(fromNumber, toNumber, messageBody, messageSid, customer, firmId);
        }
    }

    private void handleExistingCase(String fromNumber, String toNumber, String messageBody, String messageSid, Customer customer, Long firmId, List<FirmClientMapping> existingNonUnknownOpt) {
        String aiPromptToCheckCaseMapping = buildAiPromptToCheckCaseMapping(messageBody, existingNonUnknownOpt, toNumber, fromNumber);
        NewCaseCheckDto dto = openAiService.checkCaseTypeExistingOrNew(aiPromptToCheckCaseMapping);
        Long returnedCaseId = dto.getCaseId();
        String returnedCaseType = dto.getCaseType();

        System.out.println(" >> AI returned: caseId=" + returnedCaseId + ", caseType=" + returnedCaseType);

        if (returnedCaseId > 0) {
            handleExistingThread(fromNumber, toNumber, messageBody, messageSid, returnedCaseId, returnedCaseType, firmId);
        } else {
            handleNewThread(fromNumber, toNumber, messageBody, messageSid, customer, firmId, returnedCaseType);
        }
    }

    private void handleNewCase(String fromNumber, String toNumber, String messageBody, String messageSid, Customer customer, Long firmId) {
        System.out.println("No existing case mapping found");
        IntakeCheckDto intakeCheck = openAiService.checkIntakeCompleteness(messageBody);
        boolean isComplete = intakeCheck.isComplete();
        String initialCaseType = intakeCheck.getCaseType();

        System.out.println("AI returned: complete=" + isComplete + ", caseType=" + initialCaseType);

        FirmClientMapping firmClientMapping = findOrCreateFirmClientMapping(fromNumber, firmId, initialCaseType, customer);
        String safeCaseType = initialCaseType.replaceAll("\\s+", "_");
        String threadId = fromNumber + "-" + customer.getCustMail();

        List<ConversationThread> threads = conversationThreadRepository.findByFromNumberAndToNumber(toNumber, fromNumber);
        ConversationThread thread = threads.isEmpty() ? findOrCreateConversationThread(threadId, fromNumber, toNumber, customer.getCustMail()) : threads.get(0);

        if (firmClientMapping.getFirmLawyer() == null) {
            handleNoAssignedLawyer(fromNumber, toNumber, messageBody, messageSid, customer, isComplete, safeCaseType, threadId, thread);
        } else {
            handleAssignedLawyer(fromNumber, toNumber, messageBody, messageSid, firmClientMapping, safeCaseType, thread);
        }
    }

    private String buildAiPromptToCheckCaseMapping(String messageBody, List<FirmClientMapping> existingNonUnknownOpt, String toNumber, String fromNumber) {
        StringBuilder aiPromptToCheckCaseMapping = new StringBuilder("You are a legal-intake classifier. Your task is to check if the incoming SMS belongs to an existing case thread or a new one by going through client & lawyer conversation. Given the client's SMS text, you must do the following:\n"
                + "1) If the SMS belongs to an existing case thread. Please return the matching case id.\n"
                + "2) If the SMS is new and does not belong to any existing case thread, you must classify the case type into exactly ONE of: Personal Injury, Family Law, Criminal, Employment, Other.\n"
                + "3) If client is talking about some other issue which does not belong to the original case type, return caseId: 0.\n"
                + "Output strictly in JSON with fields: \"caseType\" (string), \"caseId\" (number).\n"
                + "Now process this user text:\n"
                + "\"" + messageBody + "\"");

        for (FirmClientMapping mapping : existingNonUnknownOpt) {
            if (mapping.getFirmLawyer() != null) {
                List<ConversationThread> threads = conversationThreadRepository.findByFromNumberAndToNumberAndEmail(toNumber, fromNumber, mapping.getFirmLawyer().getLawyerMail());
                for (ConversationThread thread : threads) {
                    List<Conversation> threadConversations = conversationRepository.findByConversationThread_ConversationThreadId(thread.getConversationThreadId());
                    aiPromptToCheckCaseMapping.append(" Case Id : ").append(thread.getConversationThreadId()).append(" Conversation for case id : ");
                    for (Conversation conversation : threadConversations) {
                        aiPromptToCheckCaseMapping.append(conversation.getChannel().equalsIgnoreCase("EMAIL") ? "Client : " : "Lawyer : ").append(conversation.getMessage()).append("\n");
                    }
                }
            }
        }
        return aiPromptToCheckCaseMapping.toString();
    }

    private void handleExistingThread(String fromNumber, String toNumber, String messageBody, String messageSid, Long returnedCaseId, String returnedCaseType, Long firmId) {
        Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findById(returnedCaseId);
        if (existingThreadOpt.isPresent()) {
            ConversationThread existingThread = existingThreadOpt.get();
            String existingEmail = existingThread.getEmail();
            System.out.println("Found existingThread ID=" + existingThread.getConversationThreadId() + " with email=" + existingEmail);

            try {
                emailService.sendEmail(existingEmail, "SMS from " + fromNumber, returnedCaseType, messageBody, fromNumber, toNumber, messageSid + ": Email",null);
                System.out.println("Forwarded inbound SMS to existing thread: " + existingEmail);
            } catch (Exception ex) {
                System.err.println("Error sending email to existing thread: " + ex.getMessage());
            }

            Conversation conversation = conversationService.saveConversation(fromNumber, toNumber, existingEmail, messageBody, "INCOMING", "SMS", null, returnedCaseType, existingThread.getThreadId(), messageSid, null);
            System.out.println("Saved new conversation, ID=" + conversation.getId());
        } else {
            System.err.println("No thread found with ID=" + returnedCaseId);
        }
    }

    private void handleNewThread(String fromNumber, String toNumber, String messageBody, String messageSid, Customer customer, Long firmId, String returnedCaseType) {

        String safeCaseType = returnedCaseType.replaceAll("\\s+", "_");
        FirmClientMapping newCaseMapping = findOrCreateFirmClientMapping(fromNumber, firmId, safeCaseType, customer);
        ConversationThread newThread = createNewThread(fromNumber, toNumber, customer, returnedCaseType, firmId);
        Conversation conversation = conversationService.saveConversation(fromNumber, toNumber, newThread.getEmail(), messageBody, "INCOMING", "SMS", null, returnedCaseType, newThread.getThreadId(), messageSid, newThread);

        FirmLawyer firmLawyer = newCaseMapping.getFirmLawyer();
        if (firmLawyer == null) {
            forwardEmailToFirm(customer.getCustMail(), fromNumber, returnedCaseType, messageBody, toNumber, messageSid);
        } else {
            forwardEmailToLawyer(firmLawyer.getLawyerMail(), fromNumber, returnedCaseType, messageBody, toNumber, messageSid);
        }
    }

    private FirmClientMapping findOrCreateFirmClientMapping(String fromNumber, Long firmId, String caseType, Customer customer) {
        Optional<FirmClientMapping> mappingOpt = firmClientMappingRepository.findByPhoneFirmCaseType(fromNumber, firmId, caseType);
        if (mappingOpt.isPresent()) {
            return mappingOpt.get();
        } else {
            FirmClientMapping newMapping = new FirmClientMapping();
            newMapping.setFirm(customer);
            newMapping.setFirmLawyer(null);
            newMapping.setClientPhoneNumber(fromNumber);
            // newMapping.setCaseType(caseType);
            newMapping.setCaseType(caseType.replaceAll("\\s+", "_"));

            return firmClientMappingRepository.save(newMapping);
        }
    }

    private ConversationThread createNewThread(String fromNumber, String toNumber, Customer customer, String caseType, Long firmId) {
        ConversationThread newThread = new ConversationThread();
        newThread.setPhoneNumber(fromNumber);
        newThread.setToNumber(toNumber);
        newThread.setEmail(customer.getCustMail());
        newThread.setCreatedAt(LocalDateTime.now());
        newThread.setStatus("ACTIVE");
        newThread.setCaseType(caseType);
        newThread.setThreadId(fromNumber + "-" + caseType + "-" + toNumber);
        newThread.setCustiId(firmId);
        return conversationThreadRepository.save(newThread);
    }

    private void handleNoAssignedLawyer(String fromNumber, String toNumber, String messageBody, String messageSid, Customer customer, boolean isComplete, String safeCaseType, String threadId, ConversationThread thread) {
        if (isComplete) {
            forwardEmailToFirm(customer.getCustMail(), fromNumber, safeCaseType, messageBody, toNumber, messageSid);
            conversationService.saveConversation(fromNumber, toNumber, customer.getCustMail(), messageBody, "OUTGOING", "EMAIL", "SMS from " + fromNumber, safeCaseType, threadId, "auto-forward-" + System.currentTimeMillis(), thread);
        } else {
            String autoReply = "Hello, thanks for contacting " + customer.getCustName() + "! Please reply with your name, address, and a brief description of your case.";
            smsService.sendSms(fromNumber, toNumber, autoReply);
        }
    }

    private void handleAssignedLawyer(String fromNumber, String toNumber, String messageBody, String messageSid, FirmClientMapping firmClientMapping, String safeCaseType, ConversationThread thread) {
        String additionalCaseType = openAiService.classifyCaseType(messageBody);
        if (!additionalCaseType.equalsIgnoreCase(thread.getCaseType())) {
            thread.setCaseType(additionalCaseType);
            conversationThreadRepository.save(thread);
        }
        forwardEmailToLawyer(firmClientMapping.getFirmLawyer().getLawyerMail(), fromNumber, additionalCaseType, messageBody, toNumber, messageSid);
    }

    private void forwardEmailToFirm(String firmEmail, String fromNumber, String caseType, String messageBody, String toNumber, String messageSid) {
        try {
            emailService.sendEmail(firmEmail, "SMS from " + fromNumber, caseType, messageBody, fromNumber, toNumber, messageSid + ": Email",null);
            System.out.println("[NEW] Forwarded COMPLETE SMS to " + firmEmail);
        } catch (Exception ex) {
            System.err.println("Error forwarding first SMS via email: " + ex.getMessage());
        }
    }

    private void forwardEmailToLawyer(String lawyerEmail, String fromNumber, String caseType, String messageBody, String toNumber, String messageSid) {
        try {
            emailService.sendEmail(lawyerEmail, "SMS from " + fromNumber, caseType, messageBody, fromNumber, toNumber, messageSid + ": Email",null);
            System.out.println("Email forwarded successfully to " + lawyerEmail);
        } catch (Exception ex) {
            System.err.println("Error sending email to assigned lawyer: " + ex.getMessage());
        }
    }

    private ConversationThread findOrCreateConversationThread(String threadId, String phoneNumber, String toNumber, String email) {
        return conversationThreadRepository.findByThreadId(threadId).orElseGet(() -> {
            ConversationThread newThread = new ConversationThread();
            newThread.setThreadId(threadId);
            newThread.setPhoneNumber(phoneNumber);
            newThread.setToNumber(toNumber);
            newThread.setEmail(email);
            newThread.setCreatedAt(LocalDateTime.now());
            newThread.setStatus("ACTIVE");
            newThread.setCaseType("Unknown");
            return conversationThreadRepository.save(newThread);
        });
    }

 

public void printLawyerCounts(Long firmId) {
    List<Object[]> results = firmClientMappingRepository.countByLawyerIdIsNotNullAndFirmIdGroupedByLawyerId(firmId);
    for (Object[] result : results) {
        Long count = ((Number) result[0]).longValue();
        Long lawyerId = ((Number) result[1]).longValue();
        System.out.println("Lawyer ID: " + lawyerId + " appears " + count + " times");
    }
}
}