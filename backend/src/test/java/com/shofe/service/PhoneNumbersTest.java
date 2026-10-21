package com.shofe.service;

import com.shofe.domain.entity.User;
import com.shofe.domain.enums.ActiveMode;
import com.shofe.domain.enums.Role;
import com.shofe.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumbersTest {

    @Test
    void buildsZimbabweE164FromNationalNumber() {
        assertEquals("+263712345678", PhoneNumbers.e164("ZW", "071 234 5678"));
        assertEquals("+263712345678", PhoneNumbers.e164("zw", "712345678"));
    }

    @Test
    void masksPhoneWithoutExposingTheMiddle() {
        assertEquals("+263***5678", PhoneNumbers.mask("+263712345678"));
    }

    @Test
    void rejectsUnknownCountry() {
        assertThrows(ApiException.class, () -> PhoneNumbers.requireCountry("XX"));
    }

    @Test
    void passengerAndDriverModesAreExclusiveForRiders() {
        User user = new User();
        user.setRole(Role.RIDER);
        user.setActiveMode(ActiveMode.PASSENGER);
        user.setPhone("+263711000001");
        assertTrue(user.riding());
        assertFalse(user.driving());
        assertEquals("+263711000001", user.socketName());

        user.setRole(Role.DRIVER);
        user.setActiveMode(ActiveMode.DRIVER);
        assertTrue(user.driving());
        assertFalse(user.riding());
    }
}
