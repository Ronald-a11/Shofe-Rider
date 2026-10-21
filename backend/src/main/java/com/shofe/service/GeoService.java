package com.shofe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shofe.dto.MiscDtos;
import com.shofe.dto.RideDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Value("${shofe.fare.currency:USD}")
    private String currency;

    public RideDtos.QuoteResponse route(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            String url = "https://router.project-osrm.org/route/v1/driving/"
                    + fromLng + "," + fromLat + ";" + toLng + "," + toLat
                    + "?overview=full&geometries=geojson";
            JsonNode root = get(url);
            JsonNode route = root.path("routes").path(0);
            if (route.isMissingNode()) {
                throw new IllegalStateException("No route");
            }
            double km = route.path("distance").asDouble() / 1000.0;
            int minutes = Math.max(3, (int) Math.round(route.path("duration").asDouble() / 60.0));
            List<RideDtos.LatLng> line = new ArrayList<>();
            for (JsonNode coord : route.path("geometry").path("coordinates")) {
                line.add(new RideDtos.LatLng(coord.get(1).asDouble(), coord.get(0).asDouble()));
            }
            return bare(km, minutes, line);
        } catch (Exception ex) {
            log.info("OSRM unavailable, using straight-line estimate: {}", ex.getMessage());
            double km = GeoMath.haversineKm(fromLat, fromLng, toLat, toLng) * 1.35;
            return bare(km, GeoMath.minutesForKm(km), List.of(
                    new RideDtos.LatLng(fromLat, fromLng),
                    new RideDtos.LatLng(toLat, toLng)));
        }
    }

    public List<MiscDtos.Place> search(String query) {
        List<MiscDtos.Place> places = new ArrayList<>(Places.matching(query));
        if (query == null || query.isBlank()) {
            return places;
        }
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6&countrycodes=zw&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            JsonNode root = get(url);
            for (JsonNode node : root) {
                places.add(new MiscDtos.Place(node.path("display_name").asText(), node.path("lat").asDouble(), node.path("lon").asDouble()));
            }
        } catch (Exception ex) {
            log.info("Geocoder unavailable: {}", ex.getMessage());
        }
        return places;
    }

    public MiscDtos.Place reverse(double lat, double lng) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat + "&lon=" + lng;
            JsonNode root = get(url);
            String label = root.path("display_name").asText(null);
            if (label != null && !label.isBlank()) {
                return new MiscDtos.Place(label, lat, lng);
            }
        } catch (Exception ex) {
            log.info("Reverse geocoder unavailable: {}", ex.getMessage());
        }
        return new MiscDtos.Place("Dropped pin", lat, lng);
    }

    private RideDtos.QuoteResponse bare(double km, int minutes, List<RideDtos.LatLng> line) {
        BigDecimal distance = BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
        return new RideDtos.QuoteResponse(distance, minutes, BigDecimal.ZERO, currency, false, line);
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(4))
                .header("User-Agent", "ShofeRide/0.1 (Zimbabwe)")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }
}
