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

/**
 * Africa's Talking SMS (documented fields: username, to, message).
 * https://developers.africastalking.com/docs/sms/sending
 */
@Slf4j
public class AfricasTalkingSmsGateway implements SmsGateway {

    private final String username;
    private final String apiKey;
    private final String from;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public AfricasTalkingSmsGateway(String username, String apiKey, String from) {
        this.username = username;
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(String e164Phone, String message) {
        try {
            StringBuilder form = new StringBuilder()
                    .append("username=").append(enc(username))
                    .append("&to=").append(enc(e164Phone))
                    .append("&message=").append(enc(message));
            if (from != null && !from.isBlank()) {
                form.append("&from=").append(enc(from));
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.africastalking.com/version1/messaging"))
                    .timeout(Duration.ofSeconds(12))
                    .header("apiKey", apiKey)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Africa's Talking SMS failed for {} status {}", PhoneNumbers.mask(e164Phone), response.statusCode());
                throw ApiException.badRequest("Could not send the SMS. Try again in a moment.");
            }
            log.info("Africa's Talking SMS accepted for {}", PhoneNumbers.mask(e164Phone));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Africa's Talking SMS error for {}: {}", PhoneNumbers.mask(e164Phone), ex.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SMS delivery is unavailable right now");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
