package com.example.sms_email_integration.controller;

import com.example.sms_email_integration.service.SmsService;
import com.example.sms_email_integration.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import com.example.sms_email_integration.util.EmailParser;

@RestController
@RequestMapping("/api")
public class EmailReplyController {

    private final SmsService smsService;
    private final ConversationService conversationService;

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
        @RequestParam(value = "message-id", required = false) String sg_message_id
    ) {
        // 1) Log what we got
        System.out.println("=== Incoming Email Data ===");
        System.out.println("From: " + fromAddress);
        System.out.println("To: " + toAddress);
        System.out.println("Subject: " + subject);
        System.out.println("spam_score: " + spamScore);
        System.out.println("SPF: " + spf);
        System.out.println("dkim: " + dkim);
        System.out.println("text: " + EmailParser.extractNewEmailBody(textBody));

      

        String phoneNumber = null ;
        if (!StringUtils.hasText(subject)) {
            subject = "";
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("SMS from (\\+\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(subject);
        if (matcher.find()) {
            phoneNumber = matcher.group(1); 
        }

        // 3) If phoneNumber found, send SMS
        if (phoneNumber != null) {
            
            String truncatedBody = EmailParser.extractNewEmailBody(textBody);

            String smsText = truncatedBody;

            try {
                smsService.sendSms(phoneNumber, smsText);
                System.out.println("Sent SMS reply to " + phoneNumber);

                conversationService.saveConversation(
                        phoneNumber,   // phone number
                        null,          // toNumber (not applicable for SMS)
                        fromAddress,   // lawyer email (optional)
                        truncatedBody,      // message content
                        "OUTGOING",    // direction
                        "SMS",         // channel
                        null,          // subject for SMS is null
                        phoneNumber,   // threadId
                        sg_message_id     // messageId
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