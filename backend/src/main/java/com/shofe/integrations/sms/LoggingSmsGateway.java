package com.shofe.integrations.sms;

import com.shofe.service.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;

/**
 * Used when no SMS keys are set. Does not log the message body (OTP).
 */
@Slf4j
public class LoggingSmsGateway implements SmsGateway {
    @Override
    public void send(String e164Phone, String message) {
        log.info("SMS queued locally for {} (no SMS provider keys configured)", PhoneNumbers.mask(e164Phone));
    }
}
