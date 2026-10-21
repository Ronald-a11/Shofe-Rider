package com.shofe.integrations.payments;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PaymentProviderRegistry {

    private final List<PaymentProvider> providers;
    private final PesepayProvider pesepay;
    private final StripeProvider stripe;

    public PaymentProviderRegistry(List<PaymentProvider> providers, PesepayProvider pesepay, StripeProvider stripe) {
        this.providers = providers;
        this.pesepay = pesepay;
        this.stripe = stripe;
    }

    public PaymentProvider forCountryCurrency(String countryCode, String currency) {
        String country = countryCode == null ? "ZW" : countryCode.toUpperCase(Locale.ROOT);
        String cur = currency == null ? "USD" : currency.toUpperCase(Locale.ROOT);
        if (pesepay.supports(country, cur)) {
            return pesepay;
        }
        return providers.stream()
                .filter(provider -> provider.supports(country, cur))
                .findFirst()
                .orElse(stripe);
    }

    public PaymentProvider byCode(String code) {
        return providers.stream()
                .filter(provider -> provider.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment provider: " + code));
    }

    public PesepayProvider pesepay() {
        return pesepay;
    }

    public StripeProvider stripe() {
        return stripe;
    }
}
