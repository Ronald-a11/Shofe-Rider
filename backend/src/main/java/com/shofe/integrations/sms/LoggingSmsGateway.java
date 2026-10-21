package com.shofe.integrations.sms;

import com.shofe.exception.ApiException;
import lombok.extern.slf4j.Slf4j;

/**
 * Used when no SMS keys are set. Refuses to pretend a code was delivered.
 */
@Slf4j
public class LoggingSmsGateway implements SmsGateway {
    @Override
    public void send(String e164Phone, String message) {
        log.warn("SMS provider is not configured");
        throw ApiException.unavailable("SMS is not configured. Set Twilio or Africa's Talking keys to send login codes.");
    }
}
