package com.example.sms_email_integration.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sms_email_integration.Config.TwilioConfig;
import com.example.sms_email_integration.entity.IncomingMessage;
import com.example.sms_email_integration.repository.IncomingMessageRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class SmsService {

    private final TwilioConfig twilioConfig;

    @Autowired 
    private IncomingMessageRepository incomingMessageRepository;

    public SmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public void sendSms(String to, String fromTwilio, String body) {
        Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromTwilio),
                // new PhoneNumber(twilioConfig.getFromPhoneNumber()),
                body
        ).create();

        

         IncomingMessage outgoing = new IncomingMessage();
        outgoing.setFromNumber(fromTwilio);
        outgoing.setToNumber(to);
        outgoing.setBody(body);
        outgoing.setReceivedAt(LocalDateTime.now());
        outgoing.setDirection("OUTGOING");
        
        incomingMessageRepository.save(outgoing);
    }
}
