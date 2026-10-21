package com.shofe.integrations.sms;

public interface SmsGateway {
    void send(String e164Phone, String message);
}
