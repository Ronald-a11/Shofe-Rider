package com.shofe.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FareCalculator {
    private FareCalculator() {}

    public static BigDecimal recommend(double roadKm, boolean nightOwl, BigDecimal base, BigDecimal perKm,
                                       BigDecimal nightMultiplier, BigDecimal minimum) {
        BigDecimal fare = base.add(perKm.multiply(BigDecimal.valueOf(roadKm)));
        if (nightOwl) {
            fare = fare.multiply(nightMultiplier);
        }
        if (fare.compareTo(minimum) < 0) {
            fare = minimum;
        }
        return fare.setScale(0, RoundingMode.HALF_UP);
    }
}
