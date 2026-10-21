package com.shofe.integrations.sms;

import com.shofe.exception.ApiException;
import com.shofe.service.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Twilio Messages API (documented fields only: To, From, Body).
 * https://www.twilio.com/docs/sms/send-messages
 */
@Slf4j
public class TwilioSmsGateway implements SmsGateway {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public TwilioSmsGateway(String accountSid, String authToken, String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public void send(String e164Phone, String message) {
        try {
            String form = "To=" + enc(e164Phone) + "&From=" + enc(fromNumber) + "&Body=" + enc(message);
            String basic = Base64.getEncoder().encodeToString(
                    (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Twilio SMS failed for {} status {}", PhoneNumbers.mask(e164Phone), response.statusCode());
                throw ApiException.badRequest("Could not send the SMS. Try again in a moment.");
            }
            log.info("Twilio SMS accepted for {}", PhoneNumbers.mask(e164Phone));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Twilio SMS error for {}: {}", PhoneNumbers.mask(e164Phone), ex.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS delivery is unavailable right now");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
