package com.shofe.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RideDtos {
    private RideDtos() {}

    public record QuoteRequest(
            @NotNull Double pickupLat,
            @NotNull Double pickupLng,
            @NotNull Double destinationLat,
            @NotNull Double destinationLng,
            boolean nightOwl
    ) {}

    public record LatLng(double lat, double lng) {}

    public record QuoteResponse(
            BigDecimal distanceKm,
            int durationMin,
            BigDecimal recommendedFare,
            String currency,
            boolean nightOwl,
            List<LatLng> route
    ) {}

    public record CreateRequest(
            @NotNull Double pickupLat,
            @NotNull Double pickupLng,
            @NotBlank @Size(max = 300) String pickupAddress,
            @NotNull Double destinationLat,
            @NotNull Double destinationLng,
            @NotBlank @Size(max = 300) String destinationAddress,
            @NotNull @DecimalMin("15.00") BigDecimal proposedFare,
            @NotBlank String paymentMethod,
            boolean nightOwl
    ) {}

    public record OfferRequest(
            @NotNull @DecimalMin("15.00") BigDecimal amount,
            @NotBlank String type,
            @Min(1) @Max(90) int etaMinutes,
            @Size(max = 300) String message
    ) {}

    public record OfferResponse(
            UUID id,
            UUID requestId,
            UUID driverId,
            String driverName,
            double driverRating,
            int driverRatingCount,
            int tripCount,
            String vehicleMake,
            String vehicleModel,
            String vehicleColor,
            String plateNumber,
            BigDecimal amount,
            String type,
            int etaMinutes,
            String status,
            String message,
            Instant createdAt
    ) {}

    public record RideRequestResponse(
            UUID id,
            String status,
            String riderName,
            String pickupAddress,
            String destinationAddress,
            double pickupLat,
            double pickupLng,
            double destinationLat,
            double destinationLng,
            BigDecimal distanceKm,
            int durationMin,
            BigDecimal recommendedFare,
            BigDecimal proposedFare,
            String currency,
            boolean nightOwl,
            String paymentMethod,
            Instant createdAt,
            Instant expiresAt,
            UUID rideId,
            List<OfferResponse> offers
    ) {}

    public record RideResponse(
            UUID id,
            UUID requestId,
            String status,
            BigDecimal fare,
            String currency,
            boolean nightOwl,
            String pinCode,
            boolean pinVerified,
            String shareToken,
            String sharePath,
            String pickupAddress,
            String destinationAddress,
            double pickupLat,
            double pickupLng,
            double destinationLat,
            double destinationLng,
            Party rider,
            Party driver,
            String paymentMethod,
            String paymentStatus,
            String cancelReason,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt
    ) {}

    public record Party(
            UUID id,
            String fullName,
            String phone,
            double ratingAvg,
            int ratingCount,
            String vehicleMake,
            String vehicleModel,
            String vehicleColor,
            String plateNumber,
            Double lat,
            Double lng
    ) {}

    public record StatusRequest(@NotBlank String status, @Size(max = 300) String reason) {}

    public record PinRequest(@NotBlank @Size(min = 4, max = 4) String pin) {}

    public record RateRequest(@Min(1) @Max(5) int score, @Size(max = 500) String comment) {}

    public record ChatRequest(@NotBlank @Size(max = 1000) String body) {}

    public record ChatResponse(UUID id, UUID rideId, UUID senderId, String senderName, String body, Instant sentAt) {}

    public record LocationRequest(@NotNull Double lat, @NotNull Double lng, Double heading, UUID rideId) {}

    public record TrackResponse(
            String status,
            String driverName,
            String vehicle,
            String plateNumber,
            String pickupAddress,
            String destinationAddress,
            Double driverLat,
            Double driverLng,
            boolean nightOwl
    ) {}
}
