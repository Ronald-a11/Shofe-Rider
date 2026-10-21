package com.shofe.integrations.payments;

import com.shofe.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Global cards stay behind PaymentProvider until Stripe keys exist. */
@Component
public class StripeProvider implements PaymentProvider {

    @Override
    public String code() {
        return "STRIPE";
    }

    @Override
    public boolean supports(String countryCode, String currency) {
        return false;
    }

    @Override
    public InitiateResult initiate(InitiateRequest request) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Card checkout is not available. Pay with Pesepay or cash.");
    }

    @Override
    public StatusResult checkStatus(String providerRef) {
        return new StatusResult(providerRef, "UNKNOWN", false, false);
    }
}
