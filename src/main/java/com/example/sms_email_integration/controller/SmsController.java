package com.example.sms_email_integration.controller;

import com.example.sms_email_integration.entity.Customer;
import com.example.sms_email_integration.entity.FirmClientMapping;
import com.example.sms_email_integration.entity.FirmLawyer;
import com.example.sms_email_integration.entity.IncomingMessage;
import com.example.sms_email_integration.repository.CustomerRepository;
import com.example.sms_email_integration.repository.FirmClientMappingRepository;
import com.example.sms_email_integration.repository.IncomingMessageRepository;
import com.example.sms_email_integration.service.ConversationService;
import com.example.sms_email_integration.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;





@RestController
@RequestMapping("/api")
public class SmsController {

    private final IncomingMessageRepository incomingMessageRepository;
    private final EmailService emailService;

    // New: to dynamically find the associated customer/email
    private final CustomerRepository customerRepository;

   private final ConversationService conversationService;

    private final FirmClientMappingRepository firmClientMappingRepository;

@Autowired
public SmsController(
        IncomingMessageRepository incomingMessageRepository,
        EmailService emailService,
        CustomerRepository customerRepository,
        ConversationService conversationService,
        FirmClientMappingRepository firmClientMappingRepository
) {
    this.incomingMessageRepository = incomingMessageRepository;
    this.emailService = emailService;
    this.customerRepository = customerRepository;
    this.conversationService = conversationService;
    this.firmClientMappingRepository = firmClientMappingRepository;
}


    /**
     * Twilio will POST to this endpoint when an SMS is received.
     */
    @PostMapping(value = "/incoming-sms",
                 consumes = "application/x-www-form-urlencoded")
    public void handleIncomingSms(@RequestParam("From") String fromNumber,
                                    @RequestParam("To")   String toNumber,
                                    @RequestParam("Body") String messageBody,
                                    @RequestParam("MessageSid") String messageSid,
                                    HttpServletResponse response) {

        // 1) Save incoming SMS to DB
        IncomingMessage incomingMsg = new IncomingMessage(
                fromNumber,
                toNumber,
                messageBody,
                LocalDateTime.now()
        );
        incomingMessageRepository.save(incomingMsg);

    // conversationService.saveConversation(
    //             fromNumber,        // phoneNumber (client)
    //             toNumber,          // toNumber (Twilio)
    //             null,              // email (not available in an inbound SMS)
    //             messageBody,       // content
    //             "INCOMING",        // direction
    //             "SMS",             // channel
    //             null,              // subject (not applicable for SMS)
    //             fromNumber,         // threadId (we're just using "fromNumber")
    //            messageSid         // messageId (Twilio's unique ID)
    //     );

        // 2) Log
        System.out.println("Incoming SMS from " + fromNumber + " to " + toNumber + ": " + messageBody);

        // 3) Dynamically find which customer is associated with this Twilio "toNumber"
        Optional<Customer> optionalCustomer = customerRepository.findByTwilioNumber(toNumber);

        

        if (optionalCustomer.isEmpty()) {
            System.err.println("No customer record found for Twilio number: " + toNumber);
            // Optionally: do nothing or send an alert.
            // For now, we won't send an email if we don't know who it belongs to.
            response.setContentType("text/plain");
            // return "Ok (No matching customer)";
        }

        Customer customer = optionalCustomer.get();

        Optional<FirmClientMapping> firmClientMappingOptional = firmClientMappingRepository.findByClientPhoneNumberAndCustiId(fromNumber,customer.getCusti_id());

        String senderEmail = customer.getCustMail();
        if (!firmClientMappingOptional.isEmpty()) {
            FirmClientMapping firmClientMapping = firmClientMappingOptional.get();
            FirmLawyer firmLawyer = firmClientMapping.getFirmLawyer();
            System.out.println("Firm Lawyer: " + firmLawyer.getLawyerName());
            System.out.println("Firm Lawyer Email: " + firmLawyer.getLawyerMail());
            senderEmail = firmLawyer.getLawyerMail();
        }
        String subject = "SMS from " + fromNumber + " - Test Email from SendGrid";
        String textContent =  messageBody;

        // 4) Send an email to that customer's email address
        try {
            emailService.sendEmail(
                    senderEmail,    // dynamic email address
                    subject,
                    textContent,
                    fromNumber,   // sets the custom header
                    toNumber,
                    messageSid     

            );
            System.out.println("Email forwarded successfully to " + senderEmail);
        } catch (Exception ex) {
            System.err.println("Error sending email: " + ex.getMessage());
        }

        // 5) Return "Ok"
        //response.setContentType("text/plain");
        // return "Ok";
    }
}
