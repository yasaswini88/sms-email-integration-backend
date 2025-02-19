package com.example.sms_email_integration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.dto.EmailIncomingDto;
import com.example.sms_email_integration.entity.ConversationThread;
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.EmailIncoming;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.ConversationThreadRepository;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.EmailIncomingRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailIncomingService;
import com.example.sms_email_integration.service.SmsService;
import com.example.sms_email_integration.util.EmailParser;
import com.example.sms_email_integration.util.EmailUtil;

@RestController
@RequestMapping("/api")
public class EmailReplyControllerv2 {

    private final SmsService smsService;
    private final ConversationService conversationService;

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailIncomingRepository emailIncomingRepository;

    @Autowired
    private ConversationThreadRepository conversationThreadRepository;

    @Autowired
    private EmailIncomingService emailIncomingService;

    @Autowired
    public EmailReplyControllerv2(SmsService smsService,
            ConversationService conversationService) {
        this.smsService = smsService;
        this.conversationService = conversationService;
    }

    @PostMapping("/incoming-email")
    public ResponseEntity<String> handleIncomingEmail(
            @RequestParam(value = "from", required = false) String fromAddress,
            @RequestParam(value = "to", required = false) String toAddress,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "text", required = false) String textBody,
            @RequestParam(value = "email", required = false) String emailBody,
            @RequestParam(value = "spam_score", required = false) String spamScore,
            @RequestParam(value = "SPF", required = false) String spf,
            @RequestParam(value = "dkim", required = false) String dkim,
            @RequestParam(value = "messageId", required = false) String sg_message_id,
            @RequestParam(value = "envelope", required = false) String envelope
    ) {
        // 1) Parse the "from" address into a “pure” email (strip <>, etc.)
        String pureEmail = EmailUtil.extractPureEmail(fromAddress);
        String phoneNumber = EmailUtil.extractPhoneNumberFromToField(toAddress);

        String threadId = phoneNumber + "-" + pureEmail;

        // 2) Possibly find the lawyer or firm that matches that email
        Optional<FirmLawyer> lawyerOpt = firmLawyerRepository.getLawyerByEmail(pureEmail);
        Optional<Customer> optCustomer = customerRepository.findByCustMail(pureEmail);

        // 3) Decide which Twilio number to use. (Your existing fallback logic)
         Optional<ConversationThread> maybeThread = conversationThreadRepository.findByThreadId(threadId);

    String chosenTwilioNum;
    if (maybeThread.isPresent()) {
        chosenTwilioNum = maybeThread.get().getToNumber();
        System.out.println("Found existing thread => TwilioNumber=" + chosenTwilioNum);
    } else {
        // fallback
        chosenTwilioNum = fallbackToFirstTwilio(optCustomer, lawyerOpt);
        System.out.println("No thread => fallback Twilio=" + chosenTwilioNum);
    }


        // 4) Extract the client phone number from the "To:" address 
        // String phoneNumber = EmailUtil.extractPhoneNumberFromToField(toAddress);
        if (phoneNumber == null) {
            System.err.println("No valid phone number found in 'To' field!");
            return ResponseEntity.ok("ok");
        }

        // For debugging
        System.out.println("=== Incoming Email Data ===");
        System.out.println("From: " + fromAddress);
        System.out.println("To: " + toAddress);
        System.out.println("Subject: " + subject);
        System.out.println("text: " + EmailParser.extractNewEmailBody(textBody));
        System.out.println("messageId: " + sg_message_id);

        // 5) *** STORE THIS INBOUND EMAIL in your email_incoming table. ***
        //    Also store it as a conversation if desired.
        String truncatedBody = EmailParser.extractNewEmailBody(textBody);

        // (a) Build the EmailIncoming entity
        EmailIncoming inboundEmail = new EmailIncoming();
        inboundEmail.setClientPhoneNumber(phoneNumber);
        inboundEmail.setReceivedAt(LocalDateTime.now());
        inboundEmail.setDirection("INCOMING");

        // If you found a matching lawyer
        if (lawyerOpt.isPresent()) {
            inboundEmail.setLawyer(lawyerOpt.get());
            inboundEmail.setCustiId(lawyerOpt.get().getFirm().getCusti_id());
        } else if (optCustomer.isPresent()) {
            inboundEmail.setCustiId(optCustomer.get().getCusti_id());
        }
        emailIncomingRepository.save(inboundEmail);

        // (b) Save a conversation row for the inbound email
        // String threadId = phoneNumber + "-" + pureEmail;
        conversationService.saveConversation(
                phoneNumber, // phoneNumber
                chosenTwilioNum,
                pureEmail, // lawyer email
                truncatedBody, // message content
                "INCOMING", // direction
                "EMAIL", // channel
                subject, // optional subject
                null, // caseType or "Unknown"
                threadId, // threadId
                sg_message_id, // messageId
                null
        );

        // 6) Forward the email’s text as an SMS to the client
        try {
            smsService.sendSms(phoneNumber, chosenTwilioNum, truncatedBody);
            System.out.println("Sent SMS reply to " + phoneNumber);

            // 7) Optionally create an OUTGOING conversation for that SMS
            conversationService.saveConversation(
                    phoneNumber,
                    chosenTwilioNum,
                    pureEmail,
                    truncatedBody,
                    "OUTGOING",
                    "SMS",
                    null, // subject for SMS is typically null
                    null, // caseType
                    threadId,
                    sg_message_id, // messageId
                    null
            );
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/descending")
    public ResponseEntity<List<EmailIncomingDto>> getAllInDescendingOrder() {
        List<EmailIncomingDto> dtos = emailIncomingService.getAllDescending();
        return ResponseEntity.ok(dtos);
    }


    private String fallbackToFirstTwilio(Optional<Customer> optCustomer, Optional<FirmLawyer> lawyerOpt) {
    if (optCustomer.isPresent()) {
        String rawTwilio = optCustomer.get().getTwilioNumber();
        if (rawTwilio.contains(",")) {
            return rawTwilio.split(",")[0].trim();
        } else {
            return rawTwilio;
        }
    } else if (lawyerOpt.isPresent()) {
        String rawTwilio = lawyerOpt.get().getFirm().getTwilioNumber();
        if (rawTwilio.contains(",")) {
            return rawTwilio.split(",")[0].trim();
        } else {
            return rawTwilio;
        }
    } else {
        // Hard-coded fallback if you absolutely have none
        return "+1XXXXXXXXXX";
    }
}


}
