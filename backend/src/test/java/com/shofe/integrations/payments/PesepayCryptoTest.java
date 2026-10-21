package com.shofe.integrations.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PesepayCryptoTest {

    @Test
    void roundTripsAes256CbcWithDocumentedIvRule() {
        String key = "0123456789abcdef0123456789abcdef";
        String json = "{\"amountDetails\":{\"amount\":10.00,\"currencyCode\":\"USD\"}}";
        String encrypted = PesepayCrypto.encrypt(json, key);
        assertNotEquals(json, encrypted);
        assertEquals(json, PesepayCrypto.decrypt(encrypted, key));
    }
}
