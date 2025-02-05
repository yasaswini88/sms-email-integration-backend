package com.example.sms_email_integration.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;
import com.example.sms_email_integration.Config.TwilioConfig;

@Service
public class SmsService {

    private final TwilioConfig twilioConfig;

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
    }
}
