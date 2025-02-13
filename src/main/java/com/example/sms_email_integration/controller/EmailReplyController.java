package com.example.sms_email_integration.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.SmsService;
import com.example.sms_email_integration.util.EmailParser;
import com.example.sms_email_integration.util.EmailUtil;


@RestController
@RequestMapping("/api/v1")
public class EmailReplyController {

    private final SmsService smsService;
    private final ConversationService conversationService;

    @Autowired
    private FirmLawyerRepository firmLawyerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
public EmailReplyController(SmsService smsService, ConversationService conversationService) {
    this.smsService = smsService;
    this.conversationService = conversationService;
}


    @PostMapping(
        value = "/incoming-email"
        //consumes = {"multipart/form-data"}
    )
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

            String pureEmail = EmailUtil.extractPureEmail(fromAddress);
            Optional<Customer> optCustomer = customerRepository.findByCustMail(pureEmail);

             String choosenTwilioNum = null;
    if(optCustomer.isPresent()) {
        choosenTwilioNum = optCustomer.get().getTwilioNumber(); 
    } else {
        
        // fallback or handle "no record found"
        Optional<FirmLawyer> lawyer = firmLawyerRepository.getLawyerByEmail(pureEmail);
        if(lawyer.isPresent()) {
            choosenTwilioNum = lawyer.get().getFirm().getTwilioNumber();
        } else {
            // fallback or handle "no record found"
        choosenTwilioNum = "+1XXXXXXXXXX"; 
        }
    }

        // 1) Log what we got
        System.out.println("=== Incoming Email Data ===");
        System.out.println("From: " + fromAddress);
        System.out.println("To: " + toAddress);
        System.out.println("Subject: " + subject);
        System.out.println("spam_score: " + spamScore);
        System.out.println("SPF: " + spf);
        System.out.println("dkim: " + dkim);
        System.out.println("text: " + EmailParser.extractNewEmailBody(textBody));
        System.out.println("messageId: " + sg_message_id);
        System.out.println("envelope: " + envelope);

  


// Extract phone number from the "To" field (Example: +17038620152@em4558.ravi-ai.com)
String phoneNumber = EmailUtil.extractPhoneNumberFromToField(toAddress);

String threadId = phoneNumber + "-" + pureEmail;

if (phoneNumber == null) {
    System.err.println("No valid phone number found in 'To' field!");
    return ResponseEntity.ok("ok");
}



        // 3) If phoneNumber found, send SMS
        if (phoneNumber != null) {
            
            String truncatedBody = EmailParser.extractNewEmailBody(textBody);

            String smsText = truncatedBody;

            try {
                smsService.sendSms(phoneNumber, choosenTwilioNum, smsText);
                System.out.println("Sent SMS reply to " + phoneNumber);

                System.out.println("Saving conversation from Email Reply Controller for  Line 115" + phoneNumber);

                conversationService.saveConversation(
                        phoneNumber,   // phone number
                        choosenTwilioNum,          // toNumber (not applicable for SMS)
                        pureEmail,   // lawyer email (optional)
                        truncatedBody,      // message content
                        "OUTGOING",    // direction
                        "SMS",         // channel
                        null,           // subject for SMS is null
                        null,
                     
                        threadId,   // threadId
                        sg_message_id,    // messageId
                        null
                );
                
            } catch (Exception e) {
                System.err.println("Error sending SMS: " + e.getMessage());
            }
        } else {
            System.err.println("No valid phone number found in subject!");
        }

        // 4) Return a plain "ok" response
        return ResponseEntity.ok("ok");
    }
}