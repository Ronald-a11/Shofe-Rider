package com.shofe.integrations.payments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pesepay adapter for Zimbabwe (PDF Section 5).
 * When keys are missing or demo mode is on, returns a mock redirect/poll flow.
 * Live REST shapes must be confirmed against your merchant dashboard — this client
 * does not invent undocumented fields for production submission.
 */
@Slf4j
@Component
public class PesepayProvider implements PaymentProvider {

    private final String integrationKey;
    private final String encryptionKey;
    private final String resultUrl;
    private final String returnUrl;
    private final boolean demo;
    private final Map<String, String> demoStatus = new ConcurrentHashMap<>();

    public PesepayProvider(
            @Value("${shofe.pesepay.integration-key:}") String integrationKey,
            @Value("${shofe.pesepay.encryption-key:}") String encryptionKey,
            @Value("${shofe.pesepay.result-url:http://localhost:8080/api/payments/pesepay/result}") String resultUrl,
            @Value("${shofe.pesepay.return-url:http://localhost:5173/activity}") String returnUrl,
            @Value("${shofe.pesepay.demo:true}") boolean demo) {
        this.integrationKey = integrationKey;
        this.encryptionKey = encryptionKey;
        this.resultUrl = resultUrl;
        this.returnUrl = returnUrl;
        this.demo = demo || integrationKey.isBlank() || encryptionKey.isBlank();
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
                || "ZAR".equalsIgnoreCase(currency);
    }

    @Override
    public InitiateResult initiate(InitiateRequest request) {
        if (demo) {
            String ref = "pp_demo_" + UUID.randomUUID().toString().substring(0, 10);
            demoStatus.put(ref, "PENDING");
            log.info("Pesepay DEMO initiate {} amount {} {} (keys stay server-side; result={} return={})",
                    ref, request.amount(), request.currency(), resultUrl, returnUrl);
            return new InitiateResult(
                    ref,
                    "http://localhost:5173/payments/pesepay-demo?ref=" + ref,
                    "/api/payments/status/" + ref,
                    "PENDING",
                    true);
        }
        // Live path: call Pesepay merchant REST from here once keys + docs are confirmed.
        throw new UnsupportedOperationException(
                "Live Pesepay keys are set but the HTTP client is not enabled yet. Confirm endpoints in your merchant docs, then wire them here.");
    }

    @Override
    public StatusResult checkStatus(String providerRef) {
        if (providerRef != null && providerRef.startsWith("pp_demo_")) {
            String status = demoStatus.getOrDefault(providerRef, "PENDING");
            if ("PENDING".equals(status)) {
                demoStatus.put(providerRef, "SUCCESS");
                status = "SUCCESS";
            }
            boolean success = "SUCCESS".equals(status);
            return new StatusResult(providerRef, status, true, success);
        }
        return new StatusResult(providerRef, "UNKNOWN", false, false);
    }

    public List<MethodOption> methodsFor(String currency) {
        return List.of(
                new MethodOption("ECOCASH", "EcoCash", List.of("phone")),
                new MethodOption("ONEMONEY", "OneMoney", List.of("phone")),
                new MethodOption("CARD", "Card", List.of("cardHolder")));
    }

    public void markDemoPaid(String providerRef) {
        if (providerRef != null && providerRef.startsWith("pp_demo_")) {
            demoStatus.put(providerRef, "SUCCESS");
        }
    }
}
