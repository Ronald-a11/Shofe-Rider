package com.shofe.service;

import com.shofe.dto.RideDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FareService {

    private final GeoService geoService;
    private final NightOwlService nightOwlService;

    @Value("${shofe.fare.currency}")
    private String currency;
    @Value("${shofe.fare.base}")
    private BigDecimal base;
    @Value("${shofe.fare.per-km}")
    private BigDecimal perKm;
    @Value("${shofe.fare.night-owl-multiplier}")
    private BigDecimal nightMultiplier;
    @Value("${shofe.fare.minimum}")
    private BigDecimal minimum;

    public RideDtos.QuoteResponse quote(RideDtos.QuoteRequest request) {
        boolean nightOwl = nightOwlService.active(request.nightOwl());
        RideDtos.QuoteResponse route = geoService.route(
                request.pickupLat(), request.pickupLng(), request.destinationLat(), request.destinationLng());
        BigDecimal fare = FareCalculator.recommend(route.distanceKm().doubleValue(), nightOwl, base, perKm, nightMultiplier, minimum);
        return new RideDtos.QuoteResponse(route.distanceKm(), route.durationMin(), fare, currency, nightOwl, route.route());
    }

    public String currency() {
        return currency;
    }
}
