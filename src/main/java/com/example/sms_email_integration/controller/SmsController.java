package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.entity.IncomingMessage;
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
@RequestMapping("/api")
public class SmsController {

    private final IncomingMessageRepository incomingMessageRepository;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;
    private final ConversationService conversationService;
    private final FirmClientMappingRepository firmClientMappingRepository;
    private final SmsService smsService;

    @Autowired
private OpenAiService openAiService;

@Autowired
private ConversationThreadRepository conversationThreadRepository;



    @Autowired
    public SmsController(
            IncomingMessageRepository incomingMessageRepository,
            EmailService emailService,
            CustomerRepository customerRepository,
            ConversationService conversationService,
            FirmClientMappingRepository firmClientMappingRepository,
            SmsService smsService) {
        this.incomingMessageRepository = incomingMessageRepository;
        this.emailService = emailService;
        this.customerRepository = customerRepository;
        this.conversationService = conversationService;
        this.firmClientMappingRepository = firmClientMappingRepository;
        this.smsService = smsService;
    }

@PostMapping(value = "/incoming-sms", consumes = "application/x-www-form-urlencoded")
public void handleIncomingSms(@RequestParam("From") String fromNumber,
                              @RequestParam("To") String toNumber,
                              @RequestParam("Body") String messageBody,
                              @RequestParam("MessageSid") String messageSid,
                              HttpServletResponse response) {

    // 1) Save incoming SMS to DB
    IncomingMessage incomingMsg = new IncomingMessage(fromNumber, toNumber, messageBody, LocalDateTime.now());
    incomingMessageRepository.save(incomingMsg);

    // 2) Log
    System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

    // 3) Identify the law-firm customer
    Optional<Customer> optionalCustomer = customerRepository.findByTwilioNumber(toNumber);
    if (optionalCustomer.isEmpty()) {
        System.err.println("No customer record found for Twilio number: " + toNumber);
        response.setContentType("text/plain");
        return;
    }

    Customer customer = optionalCustomer.get();
    Optional<FirmClientMapping> firmClientMappingOptional =
            firmClientMappingRepository.findByClientPhoneNumberAndCustiId(fromNumber, customer.getCusti_id());

    // 4) If no mapping => brand new client => send auto reply
    if (firmClientMappingOptional.isEmpty()) {
        FirmClientMapping newMapping = new FirmClientMapping();
        newMapping.setFirm(customer);
        newMapping.setFirmLawyer(null);
        newMapping.setClientPhoneNumber(fromNumber);
        firmClientMappingRepository.save(newMapping);

        String autoReply = "Hello, thanks for contacting "
                + customer.getCustName()
                + "! Please reply with your name, address, and a brief description of your case.";

        smsService.sendSms(fromNumber, toNumber, autoReply);

        String newThreadId = fromNumber + "-" + customer.getCustMail();
        conversationService.saveConversation(
                fromNumber, toNumber, customer.getCustMail(), autoReply,
                "OUTGOING", "SMS", null, newThreadId, "auto-" + System.currentTimeMillis()
        );
        return;
    }

    // 5) If mapping present => check assigned lawyer
    FirmClientMapping firmClientMapping = firmClientMappingOptional.get();
    FirmLawyer firmLawyer = firmClientMapping.getFirmLawyer();

    // *** 5-A) If lawyer == null => classify the user’s message
    if (firmLawyer == null) {
        // (A) Use OpenAI to detect case type
        String userText = messageBody;
        String detectedCaseType = openAiService.classifyCaseType(userText);

        System.out.println("[SmsController] openAiService returned: " + detectedCaseType);

        // (B) find/create thread
        String threadId = fromNumber + "-" + customer.getCustMail();
        ConversationThread thread = findOrCreateConversationThread(
            threadId, fromNumber, toNumber, customer.getCustMail()
        );

        // (C) save caseType
        thread.setCaseType(detectedCaseType);
        conversationThreadRepository.save(thread);

        System.out.println("Detected case type: " + detectedCaseType);

        System.out.println("[SmsController] Updated thread " + thread.getConversationThreadId()
                               + " with caseType='" + detectedCaseType + "'");
    }

    // 5-B) Now forward the SMS to the assignedEmail
    String senderEmail = (firmLawyer == null)
                         ? customer.getCustMail()   // default firm email
                         : firmLawyer.getLawyerMail();
    String subject = "SMS from " + fromNumber;
    String textContent = messageBody;

    // Forward via email
    try {
        emailService.sendEmail(senderEmail, subject, textContent, fromNumber, toNumber, messageSid);
        System.out.println("Email forwarded successfully to " + senderEmail);
    } catch (Exception ex) {
        System.err.println("Error sending email: " + ex.getMessage());
    }
}


private ConversationThread findOrCreateConversationThread(String threadId,
                                                          String phoneNumber,
                                                          String toNumber,
                                                          String email) {
    Optional<ConversationThread> existingThreadOpt = conversationThreadRepository.findByThreadId(threadId);
    if (existingThreadOpt.isPresent()) {
        return existingThreadOpt.get();
    }
    ConversationThread newThread = new ConversationThread();
    newThread.setThreadId(threadId);
    newThread.setPhoneNumber(phoneNumber);
    newThread.setToNumber(toNumber);
    newThread.setEmail(email);
    newThread.setCreatedAt(LocalDateTime.now());
    newThread.setStatus("ACTIVE");
    return conversationThreadRepository.save(newThread);
}

}
