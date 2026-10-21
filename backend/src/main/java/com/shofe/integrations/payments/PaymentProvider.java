package com.shofe.integrations.payments;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Provider-agnostic payment port (PDF Section 5).
 * Secrets stay on the server; clients only receive redirect URLs / public status.
 */
public interface PaymentProvider {

    String code();

    boolean supports(String countryCode, String currency);

    InitiateResult initiate(InitiateRequest request);

    StatusResult checkStatus(String providerRef);

    record InitiateRequest(
            UUID internalPaymentId,
            BigDecimal amount,
            String currency,
            String customerEmail,
            String customerPhone,
            String description,
            String idempotencyKey
    ) {}

    record InitiateResult(
            String providerRef,
            String redirectUrl,
            String pollUrl,
            String status,
            boolean demo
    ) {}

    record StatusResult(String providerRef, String status, boolean finalState, boolean success) {}

    record MethodOption(String code, String label, List<String> requiredFields) {}
}
