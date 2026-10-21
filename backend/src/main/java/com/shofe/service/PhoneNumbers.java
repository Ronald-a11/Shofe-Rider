package com.shofe.service;

import com.shofe.exception.ApiException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * E.164 helpers for Southern Africa-first country numbers.
 */
public final class PhoneNumbers {
    private PhoneNumbers() {}

    public record Country(String iso, String name, String dial, String placeholder) {}

    public static final List<Country> COUNTRIES = List.of(
            new Country("ZW", "Zimbabwe", "+263", "71 234 5678"),
            new Country("ZA", "South Africa", "+27", "82 123 4567"),
            new Country("ZM", "Zambia", "+260", "97 123 4567"),
            new Country("MW", "Malawi", "+265", "99 123 4567"),
            new Country("MZ", "Mozambique", "+258", "84 123 4567"),
            new Country("BW", "Botswana", "+267", "71 123 456"),
            new Country("NA", "Namibia", "+264", "81 123 4567"),
            new Country("TZ", "Tanzania", "+255", "71 123 4567"),
            new Country("KE", "Kenya", "+254", "712 345678"),
            new Country("NG", "Nigeria", "+234", "801 234 5678"),
            new Country("GB", "United Kingdom", "+44", "7400 123456"),
            new Country("US", "United States", "+1", "202 555 0100")
    );

    private static final Map<String, Country> BY_ISO = COUNTRIES.stream()
            .collect(Collectors.toMap(c -> c.iso(), c -> c));

    public static Country requireCountry(String iso) {
        if (iso == null || iso.isBlank()) {
            return BY_ISO.get("ZW");
        }
        Country country = BY_ISO.get(iso.trim().toUpperCase(Locale.ROOT));
        if (country == null) {
            throw ApiException.badRequest("That country is not supported yet");
        }
        return country;
    }

    public static String e164(String countryIso, String nationalNumber) {
        Country country = requireCountry(countryIso);
        String digits = digitsOnly(nationalNumber);
        if (digits.isBlank()) {
            throw ApiException.badRequest("Enter your mobile number");
        }
        if (digits.startsWith(country.dial().substring(1))) {
            digits = digits.substring(country.dial().length() - 1);
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() < 7 || digits.length() > 12) {
            throw ApiException.badRequest("That number does not look like a valid mobile");
        }
        return country.dial() + digits;
    }

    public static String mask(String e164) {
        if (e164 == null || e164.length() < 6) {
            return "***";
        }
        return e164.substring(0, Math.min(4, e164.length() - 4)) + "***" + e164.substring(e164.length() - 4);
    }

    public static String digitsOnly(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
