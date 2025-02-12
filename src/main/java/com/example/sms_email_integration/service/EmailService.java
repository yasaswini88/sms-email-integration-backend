package com.example.sms_email_integration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.sms_email_integration.entity.ConversationThread;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

@Service
public class EmailService {

    @Value("${sendgrid.apiKey}")
    private String sendGridApiKey;

    private final ConversationService conversationService;

    public EmailService(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    public void sendEmail(String toEmail, String subject,String CaseType, String textContent, String fromNumber,
    String twilioNumber,
    String twilioMessageSid,
    ConversationThread useConversationThread) 
    throws Exception {
        // "from" must match a verified sender or domain in your SendGrid account
        Email from = new Email("admin@ravi-ai.com");          // same as your Node code
        Email to = new Email(toEmail);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);

        // Set replyTo to match Node code
        mail.setReplyTo(new Email(fromNumber+"@em4558.ravi-ai.com"));

        // Add personalization (for custom headers)
        Personalization personalization = new Personalization();
        personalization.addTo(to);
        // Add a custom header "X-From-Phone-Number"
        personalization.addHeader("X-From-Phone-Number", fromNumber);
        mail.addPersonalization(personalization);

        // Add content
        Content content = new Content("text/plain", textContent);
        mail.addContent(content);

String newThreadId = fromNumber + "-" + toEmail;
       
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);


            System.out.println("Sent email to " + toEmail);

            System.out.println("Saving conversation from Email Service for Line 65" + fromNumber);
            
            conversationService.saveConversation(
                    fromNumber,       // phoneNumber
                    twilioNumber,   // toNumber
                    toEmail,          // email
                    textContent,      // message content
                    "OUTGOING",       // direction
                    "EMAIL",          // channelc
                    subject,          // subject
                        CaseType,     // caseType
                    newThreadId,
                    twilioMessageSid,
                    useConversationThread
            );


        } catch (Exception ex) {
            throw new Exception("Error sending email via SendGrid", ex);
        }
    }

    public void sendVerificationCode(String lawyerEmail, String code) throws Exception {
    // Build the email
    Email from = new Email("admin@ravi-ai.com"); // Must be verified in SendGrid
    Email to = new Email(lawyerEmail);

    Mail mail = new Mail();
    mail.setFrom(from);
    mail.setSubject("Your 4-digit login code");

    // No "replyTo" or special headers needed for a login code
    Personalization personalization = new Personalization();
    personalization.addTo(to);
    mail.addPersonalization(personalization);

    // Create a simple text message body
    String textBody = "Hello,\n\n"
            + "Your login verification code is: " + code + "\n"
            + "This code will expire in 2 minutes.\n\n"
            + "Regards,\nYour App Team";

    Content content = new Content("text/plain", textBody);
    mail.addContent(content);

    // Send the email via SendGrid
    SendGrid sg = new SendGrid(sendGridApiKey);
    Request request = new Request();
    try {
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        sg.api(request);

        System.out.println("Sent verification code email to " + lawyerEmail);
    } catch (Exception ex) {
        throw new Exception("Error sending verification code via SendGrid", ex);
    }



   
}


}
