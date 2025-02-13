package com.example.sms_email_integration.controller;

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
import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmLawyerRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailIncomingService;
import com.example.sms_email_integration.service.SmsService;
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
    private EmailIncomingService emailIncomingService;

    @Autowired
    public EmailReplyControllerv2(SmsService smsService, 
                                ConversationService conversationService) {
        this.smsService = smsService;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/incoming-email")
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
        // 1) Parse the "fromAddress" to get the pure email
        String pureEmail = EmailUtil.extractPureEmail(fromAddress); // your existing utility
        System.out.println("Incoming Email from: " + pureEmail);

        // 2) Identify the phone number from "toAddress"
        String phoneNumber = EmailUtil.extractPhoneNumberFromToField(toAddress);
        if (phoneNumber == null) {
            System.err.println("No valid phone number found in 'To' field!");
            return ResponseEntity.ok("ok");
        }

        // 3) Check if the email is from a known Lawyer
        FirmLawyer matchedLawyer = null;
        Optional<FirmLawyer> lawyerOpt = firmLawyerRepository.getLawyerByEmail(pureEmail);
        if (lawyerOpt.isPresent()) {
            matchedLawyer = lawyerOpt.get();
            System.out.println("Matched lawyer: " + matchedLawyer.getLawyerMail() 
                               + " with ID=" + matchedLawyer.getLawyerId());
        }

        // 4) If it is from the lawyer, we have the firm from matchedLawyer.getFirm()
        Long firmId = null;
        if (matchedLawyer != null && matchedLawyer.getFirm() != null) {
            firmId = matchedLawyer.getFirm().getCusti_id();
        } else {
            // Possibly fallback to see if from main firm or do something else
            // Or if from a 3rd party email, handle differently
            // For example:
            Optional<Customer> optCustomer = customerRepository.findByCustMail(pureEmail);
            if (optCustomer.isPresent()) {
                firmId = optCustomer.get().getCusti_id();
            }
        }

        if (firmId == null) {
            // We do not have a recognized firm, fallback logic if necessary
            firmId = 0L; // or skip storing
        }

        // 5) Now store the EmailIncoming record 
        //    only if we recognized at least some firm and a phoneNumber
        emailIncomingService.createEmailIncoming(
                phoneNumber,     // client phone
                matchedLawyer,   // can be null if not found
                firmId           // might be 0 if not found
        );

        // 6) Then proceed with your existing logic 
        //    to send SMS reply, create conversation, etc.
        // (unchanged code omitted for brevity)

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/descending")
    public ResponseEntity<List<EmailIncomingDto>> getAllInDescendingOrder() {
        List<EmailIncomingDto> dtos = emailIncomingService.getAllDescending();
        return ResponseEntity.ok(dtos);
    }

    
}

