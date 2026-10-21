package com.shofe.integrations.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shofe.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pesepay redirect checkout using documented initiate + check-payment endpoints.
 * @see <a href="https://developers.pesepay.com/api/initiate-transaction/">Initiate Transaction</a>
 * @see <a href="https://developers.pesepay.com/api/check-payment-status/">Check Payment Status</a>
 */
@Slf4j
@Component
public class PesepayProvider implements PaymentProvider {

    private static final Set<String> TERMINAL = Set.of(
            "SUCCESS", "FAILED", "CANCELLED", "CANCELED", "REVERSED", "EXPIRED", "TIMEDOUT", "DECLINED");

    private final String integrationKey;
    private final String encryptionKey;
    private final String resultUrl;
    private final String returnUrl;
    private final boolean sandbox;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public PesepayProvider(
            @Value("${shofe.pesepay.integration-key:}") String integrationKey,
            @Value("${shofe.pesepay.encryption-key:}") String encryptionKey,
            @Value("${shofe.pesepay.result-url:http://localhost:8080/api/payments/pesepay/result}") String resultUrl,
            @Value("${shofe.pesepay.return-url:http://localhost:5173/activity}") String returnUrl,
            @Value("${shofe.pesepay.sandbox:true}") boolean sandbox,
            ObjectMapper mapper) {
        this.integrationKey = integrationKey == null ? "" : integrationKey.trim();
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
        this.resultUrl = resultUrl;
        this.returnUrl = returnUrl;
        this.sandbox = sandbox;
        this.mapper = mapper;
        if (configured()) {
            log.info("Pesepay {} client ready", sandbox ? "sandbox" : "live");
        } else {
            log.warn("Pesepay keys are not set; checkout will be refused");
        }
    }

    @Override
    public String code() {
        return "PESEPAY";
    }

    @Override
    public boolean supports(String countryCode, String currency) {
        return "ZW".equalsIgnoreCase(countryCode)
                || "USD".equalsIgnoreCase(currency)
                || "ZWG".equalsIgnoreCase(currency)
                || "ZIG".equalsIgnoreCase(currency)
                || "ZAR".equalsIgnoreCase(currency);
    }

    public boolean configured() {
        return StringUtils.hasText(integrationKey) && encryptionKey.length() >= 32;
    }

    public boolean matchesCallbackAuth(String authorization) {
        return configured() && StringUtils.hasText(authorization) && integrationKey.equals(authorization.trim());
    }

    @Override
    public InitiateResult initiate(InitiateRequest request) {
        requireKeys();
        try {
            ObjectNode body = mapper.createObjectNode();
            ObjectNode amountDetails = body.putObject("amountDetails");
            amountDetails.set("amount", mapper.getNodeFactory().numberNode(request.amount()));
            amountDetails.put("currencyCode", request.currency());
            body.put("reasonForPayment", request.description());
            body.put("resultUrl", resultUrl);
            body.put("returnUrl", returnUrl);
            if (StringUtils.hasText(request.idempotencyKey())) {
                body.put("merchantReference", request.idempotencyKey());
            }
            String encrypted = PesepayCrypto.encrypt(mapper.writeValueAsString(body), encryptionKey);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/payments/initiate"))
                    .timeout(Duration.ofSeconds(25))
                    .header("authorization", integrationKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"payload\":\"" + encrypted + "\"}"))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode decrypted = decryptBody(response);
            String ref = text(decrypted, "referenceNumber");
            String redirect = text(decrypted, "redirectUrl");
            String poll = text(decrypted, "pollUrl");
            if (!StringUtils.hasText(ref) || !StringUtils.hasText(redirect)) {
                throw ApiException.badRequest("Pesepay did not return a checkout URL");
            }
            return new InitiateResult(ref, redirect, poll, "PENDING", false);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Pesepay initiate failed: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not start Pesepay checkout");
        }
    }

    @Override
    public StatusResult checkStatus(String providerRef) {
        if (!StringUtils.hasText(providerRef)) {
            return new StatusResult(providerRef, "UNKNOWN", false, false);
        }
        requireKeys();
        try {
            String url = baseUrl() + "/payments/check-payment?referenceNumber="
                    + URLEncoder.encode(providerRef, StandardCharsets.UTF_8);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("authorization", integrationKey)
                    .header("content-type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode decrypted = decryptBody(response);
            String status = text(decrypted, "transactionStatus").toUpperCase(Locale.ROOT);
            boolean terminal = TERMINAL.contains(status);
            return new StatusResult(providerRef, status.isBlank() ? "UNKNOWN" : status, terminal, "SUCCESS".equals(status));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("Pesepay status check failed: {}", ex.getMessage());
            return new StatusResult(providerRef, "UNKNOWN", false, false);
        }
    }

    public List<MethodOption> methodsFor(String currency) {
        return List.of(
                new MethodOption("ECOCASH", "EcoCash", List.of("phone")),
                new MethodOption("ONEMONEY", "OneMoney", List.of("phone")),
                new MethodOption("CARD", "Card", List.of("cardHolder")));
    }

    private void requireKeys() {
        if (!configured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Pesepay is not configured");
        }
    }

    private String baseUrl() {
        return sandbox
                ? "https://api.test.sandbox.pesepay.com/payments-engine/v1"
                : "https://api.pesepay.com/api/payments-engine/v1";
    }

    private JsonNode decryptBody(HttpResponse<String> response) throws Exception {
        JsonNode root = mapper.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
        if (response.statusCode() >= 400) {
            String message = root.path("message").asText("Pesepay request failed");
            throw new ApiException(HttpStatus.BAD_GATEWAY, message);
        }
        String payload = root.path("payload").asText();
        if (!StringUtils.hasText(payload)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Pesepay returned an empty payload");
        }
        return mapper.readTree(PesepayCrypto.decrypt(payload, encryptionKey));
    }

    private static String text(JsonNode node, String field) {
        return node == null || node.path(field).isMissingNode() ? "" : node.path(field).asText("");
    }
}
