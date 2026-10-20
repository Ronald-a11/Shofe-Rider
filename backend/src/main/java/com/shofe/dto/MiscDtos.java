package com.shofe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MiscDtos {
    private MiscDtos() {}

    public record Place(String label, double lat, double lng) {}

    public record OnlineRequest(@NotNull Double lat, @NotNull Double lng, boolean roadsideOptIn) {}

    public record DriverProfileRequest(
            @NotBlank String licenseNumber,
            @NotBlank String vehicleMake,
            @NotBlank String vehicleModel,
            @NotBlank String vehicleColor,
            @NotBlank String plateNumber,
            @NotNull Integer vehicleYear,
            boolean roadsideOptIn
    ) {}

    public record VerificationRequest(@NotBlank String status, @Size(max = 300) String reason) {}

    public record SosRequest(@NotNull Double lat, @NotNull Double lng, UUID rideId, @Size(max = 500) String note) {}

    public record SosResponse(UUID id, Instant createdAt, String message) {}

    public record RoadsideCreate(
            @NotBlank String type,
            @NotNull Double lat,
            @NotNull Double lng,
            @NotBlank String address,
            @Size(max = 500) String details,
            @NotNull BigDecimal fare
    ) {}

    public record RoadsideResponse(
            UUID id,
            String type,
            String status,
            String address,
            String details,
            double lat,
            double lng,
            BigDecimal fare,
            String currency,
            String riderName,
            String providerName,
            Double providerLat,
            Double providerLng,
            Instant createdAt
    ) {}

    public record RoadsideStatusRequest(@NotBlank String status) {}

    public record AdminStats(long users, long driversPending, long openRequests, long activeRides, long completedRides, long sosEvents) {}

    public record TeleportRequest(@NotNull Double lat, @NotNull Double lng) {}

    public record NightOwlStatus(
            boolean active,
            boolean manualOn,
            boolean scheduleWindow,
            boolean solarNight,
            String zone,
            int startHour,
            int endHour,
            EmergencyInfo emergency,
            String uiHint
    ) {}

    public record CheckInRequest(boolean ok, Double lat, Double lng, UUID rideId, @Size(max = 300) String note) {}

    public record CheckInResponse(UUID id, boolean ok, Instant createdAt, String message) {}

    public record HelpPlace(String type, String name, double lat, double lng, double distanceKm, boolean open24h) {}

    public record EmergencyInfo(String countryCode, String police, String ambulance, String fire, String label) {}

    public record PayStartRequest(String provider) {}

    public record WalletTopUpRequest(@NotNull BigDecimal amount) {}
}
