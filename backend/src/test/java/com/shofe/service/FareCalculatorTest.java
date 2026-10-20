package com.shofe.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareCalculatorTest {

    @Test
    void appliesBaseDistanceAndNightOwlMultiplier() {
        BigDecimal day = FareCalculator.recommend(10, false,
                new BigDecimal("20"), new BigDecimal("9"), new BigDecimal("1.15"), new BigDecimal("25"));
        BigDecimal night = FareCalculator.recommend(10, true,
                new BigDecimal("20"), new BigDecimal("9"), new BigDecimal("1.15"), new BigDecimal("25"));
        assertEquals(new BigDecimal("110"), day);
        assertEquals(new BigDecimal("127"), night);
    }

    @Test
    void enforcesMinimumFare() {
        BigDecimal fare = FareCalculator.recommend(0.2, false,
                new BigDecimal("20"), new BigDecimal("9"), new BigDecimal("1.15"), new BigDecimal("25"));
        assertEquals(new BigDecimal("25"), fare);
    }

    @Test
    void haversineIsShortInsideTheSameNeighbourhood() {
        double km = GeoMath.haversineKm(-26.2041, 28.0473, -26.1988, 28.0502);
        assertTrue(km > 0.3 && km < 1.2);
    }
}
