package com.shofe.config;

import com.shofe.integrations.sms.AfricasTalkingSmsGateway;
import com.shofe.integrations.sms.LoggingSmsGateway;
import com.shofe.integrations.sms.SmsGateway;
import com.shofe.integrations.sms.TwilioSmsGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SmsConfig {

    @Bean
    public SmsGateway smsGateway(
            @Value("${shofe.sms.twilio.account-sid:}") String twilioSid,
            @Value("${shofe.sms.twilio.auth-token:}") String twilioToken,
            @Value("${shofe.sms.twilio.from:}") String twilioFrom,
            @Value("${shofe.sms.africastalking.username:}") String atUser,
            @Value("${shofe.sms.africastalking.api-key:}") String atKey,
            @Value("${shofe.sms.africastalking.from:}") String atFrom) {
        if (!twilioSid.isBlank() && !twilioToken.isBlank() && !twilioFrom.isBlank()) {
            log.info("SMS provider: Twilio");
            return new TwilioSmsGateway(twilioSid, twilioToken, twilioFrom);
        }
        if (!atUser.isBlank() && !atKey.isBlank()) {
            log.info("SMS provider: Africa's Talking");
            return new AfricasTalkingSmsGateway(atUser, atKey, atFrom);
        }
        log.info("SMS provider: local queue (set TWILIO_* or AT_* keys to send real messages)");
        return new LoggingSmsGateway();
    }
}
