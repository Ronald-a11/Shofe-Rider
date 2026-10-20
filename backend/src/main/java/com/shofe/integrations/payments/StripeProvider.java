package com.shofe.integrations.payments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Global cards stub (PDF Section 4.5). Swap in real Stripe SDK when keys exist. */
@Slf4j
@Component
public class StripeProvider implements PaymentProvider {

    @Override
    public String code() {
        return "STRIPE";
    }

    @Override
    public boolean supports(String countryCode, String currency) {
        return true;
    }

    @Override
    public InitiateResult initiate(InitiateRequest request) {
        String ref = "pi_stub_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Stripe stub initiate {} for {} {}", ref, request.amount(), request.currency());
        return new InitiateResult(ref, null, "/api/payments/status/" + ref, "AWAITING_PAYMENT", true);
    }

    @Override
    public StatusResult checkStatus(String providerRef) {
        return new StatusResult(providerRef, "AWAITING_PAYMENT", false, false);
    }
}
