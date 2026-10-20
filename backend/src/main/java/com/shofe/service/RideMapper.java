package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.Payment;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.RideOffer;
import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.entity.User;
import com.shofe.dto.RideDtos;
import com.shofe.repo.PaymentRepository;
import com.shofe.repo.RideOfferRepository;
import com.shofe.repo.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RideMapper {

    private final RideOfferRepository offers;
    private final RideRepository rides;
    private final PaymentRepository payments;

    public RideDtos.OfferResponse toOffer(RideOffer offer) {
        User driver = offer.getDriver();
        DriverProfile profile = driver.getDriverProfile();
        return new RideDtos.OfferResponse(
                offer.getId(),
                offer.getRequest().getId(),
                driver.getId(),
                driver.getFullName(),
                driver.getRatingAvg(),
                driver.getRatingCount(),
                profile == null ? 0 : profile.getTripCount(),
                profile == null ? null : profile.getVehicleMake(),
                profile == null ? null : profile.getVehicleModel(),
                profile == null ? null : profile.getVehicleColor(),
                profile == null ? null : profile.getPlateNumber(),
                offer.getAmount(),
                offer.getType().name(),
                offer.getEtaMinutes(),
                offer.getStatus().name(),
                offer.getMessage(),
                offer.getCreatedAt());
    }

    public RideDtos.RideRequestResponse toRequest(RideRequest request, boolean includeOffers) {
        List<RideDtos.OfferResponse> offerDtos = includeOffers
                ? offers.findByRequestIdOrderByAmountAscCreatedAtAsc(request.getId()).stream().map(this::toOffer).toList()
                : List.of();
        UUID rideId = rides.findByRequestId(request.getId()).map(Ride::getId).orElse(null);
        return new RideDtos.RideRequestResponse(
                request.getId(),
                request.getStatus().name(),
                request.getRider().getFullName(),
                request.getPickupAddress(),
                request.getDestinationAddress(),
                request.getPickupLat(),
                request.getPickupLng(),
                request.getDestinationLat(),
                request.getDestinationLng(),
                request.getDistanceKm(),
                request.getDurationMin(),
                request.getRecommendedFare(),
                request.getProposedFare(),
                request.getCurrency(),
                request.isNightOwl(),
                request.getPaymentMethod().name(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                rideId,
                offerDtos);
    }

    public RideDtos.RideResponse toRide(Ride ride, User viewer) {
        boolean riderView = viewer != null && viewer.getId().equals(ride.getRider().getId());
        Payment payment = payments.findFirstByRideIdOrderByCreatedAtDesc(ride.getId()).orElse(null);
        DriverProfile profile = ride.getDriver().getDriverProfile();
        return new RideDtos.RideResponse(
                ride.getId(),
                ride.getRequest().getId(),
                ride.getStatus().name(),
                ride.getFare(),
                ride.getCurrency(),
                ride.isNightOwl(),
                riderView ? ride.getPinCode() : null,
                ride.isPinVerified(),
                ride.getShareToken(),
                "/track/" + ride.getShareToken(),
                ride.getRequest().getPickupAddress(),
                ride.getRequest().getDestinationAddress(),
                ride.getRequest().getPickupLat(),
                ride.getRequest().getPickupLng(),
                ride.getRequest().getDestinationLat(),
                ride.getRequest().getDestinationLng(),
                party(ride.getRider(), null),
                party(ride.getDriver(), profile),
                payment != null ? payment.getMethod().name() : ride.getRequest().getPaymentMethod().name(),
                payment == null ? null : payment.getStatus().name(),
                ride.getCancelReason(),
                ride.getCreatedAt(),
                ride.getStartedAt(),
                ride.getCompletedAt());
    }

    public RideDtos.TrackResponse toTrack(Ride ride) {
        DriverProfile profile = ride.getDriver().getDriverProfile();
        String vehicle = profile == null ? "Vehicle" : profile.getVehicleColor() + " " + profile.getVehicleMake() + " " + profile.getVehicleModel();
        return new RideDtos.TrackResponse(
                ride.getStatus().name(),
                firstName(ride.getDriver().getFullName()),
                vehicle,
                profile == null ? null : profile.getPlateNumber(),
                ride.getRequest().getPickupAddress(),
                ride.getRequest().getDestinationAddress(),
                profile == null ? null : profile.getLastLat(),
                profile == null ? null : profile.getLastLng(),
                ride.isNightOwl());
    }

    private RideDtos.Party party(User user, DriverProfile profile) {
        return new RideDtos.Party(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getRatingAvg(),
                user.getRatingCount(),
                profile == null ? null : profile.getVehicleMake(),
                profile == null ? null : profile.getVehicleModel(),
                profile == null ? null : profile.getVehicleColor(),
                profile == null ? null : profile.getPlateNumber(),
                profile == null ? null : profile.getLastLat(),
                profile == null ? null : profile.getLastLng());
    }

    private String firstName(String fullName) {
        int space = fullName.indexOf(' ');
        return space < 0 ? fullName : fullName.substring(0, space);
    }
}
