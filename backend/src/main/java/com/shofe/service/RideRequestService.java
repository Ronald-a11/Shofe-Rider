package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.PaymentMethod;
import com.shofe.domain.enums.RequestStatus;
import com.shofe.domain.enums.Role;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.DriverProfileRepository;
import com.shofe.repo.RideRequestRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideRequestService {

    private final CurrentUserService currentUserService;
    private final RideRequestRepository requests;
    private final DriverProfileRepository drivers;
    private final FareService fareService;
    private final NightOwlService nightOwlService;
    private final RideMapper rideMapper;
    private final RideSockets sockets;
    private final NotificationService notifications;
    private final DemoOfferSimulator demoOfferSimulator;

    @Value("${shofe.matching.radius-km}")
    private double radiusKm;
    @Value("${shofe.matching.night-owl-min-rating}")
    private double nightOwlMinRating;
    @Value("${shofe.matching.request-ttl-minutes}")
    private long ttlMinutes;

    @Transactional
    public RideDtos.RideRequestResponse create(RideDtos.CreateRequest body) {
        User rider = currentUserService.require();
        if (rider.getRole() == Role.DRIVER) {
            throw ApiException.badRequest("Switch to a rider account to request a trip");
        }
        RideDtos.QuoteResponse quote = fareService.quote(new RideDtos.QuoteRequest(
                body.pickupLat(), body.pickupLng(), body.destinationLat(), body.destinationLng(), body.nightOwl()));
        if (body.proposedFare().compareTo(new BigDecimal("15")) < 0) {
            throw ApiException.badRequest("Proposed fare is too low");
        }
        RideRequest request = new RideRequest();
        request.setRider(rider);
        request.setPickupLat(body.pickupLat());
        request.setPickupLng(body.pickupLng());
        request.setPickupAddress(body.pickupAddress().trim());
        request.setDestinationLat(body.destinationLat());
        request.setDestinationLng(body.destinationLng());
        request.setDestinationAddress(body.destinationAddress().trim());
        request.setDistanceKm(quote.distanceKm());
        request.setDurationMin(quote.durationMin());
        request.setRecommendedFare(quote.recommendedFare());
        request.setProposedFare(body.proposedFare());
        request.setCurrency(quote.currency());
        request.setNightOwl(quote.nightOwl());
        request.setPaymentMethod(PaymentMethod.valueOf(body.paymentMethod()));
        request.setExpiresAt(Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
        requests.save(request);

        List<DriverProfile> matched = match(request);
        RideDtos.RideRequestResponse dto = rideMapper.toRequest(request, false);
        List<String> emails = matched.stream().map(profile -> profile.getUser().getEmail()).toList();
        for (DriverProfile driver : matched) {
            notifications.push(driver.getUser(), "New Shofe request",
                    request.getPickupAddress() + " → " + request.getDestinationAddress());
        }
        demoOfferSimulator.schedule(request.getId());
        Tx.afterCommit(() -> {
            for (String email : emails) {
                sockets.requestToDriver(email, dto);
            }
        });
        return dto;
    }

    @Transactional(readOnly = true)
    public RideDtos.RideRequestResponse get(UUID id) {
        RideRequest request = requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
        assertCanView(request, currentUserService.require());
        return rideMapper.toRequest(request, true);
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideRequestResponse> mine() {
        User user = currentUserService.require();
        return requests.findByRiderIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(request -> rideMapper.toRequest(request, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideRequestResponse> openForDriver() {
        User user = currentUserService.require();
        DriverProfile profile = user.getDriverProfile();
        if (profile == null || profile.getLastLat() == null) {
            return List.of();
        }
        return requests.findByStatusInOrderByCreatedAtDesc(EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED))
                .stream()
                .filter(request -> within(profile, request))
                .filter(request -> !request.isNightOwl() || user.getRatingAvg() >= nightOwlMinRating || user.getRatingCount() == 0)
                .map(request -> rideMapper.toRequest(request, false))
                .toList();
    }

    @Transactional
    public RideDtos.RideRequestResponse cancel(UUID id) {
        User user = currentUserService.require();
        RideRequest request = requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (!request.getRider().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the rider can cancel this request");
        }
        if (request.getStatus() == RequestStatus.ACCEPTED) {
            throw ApiException.badRequest("This request already became a ride. Cancel the ride instead.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED || request.getStatus() == RequestStatus.EXPIRED) {
            return rideMapper.toRequest(request, true);
        }
        request.setStatus(RequestStatus.CANCELLED);
        return rideMapper.toRequest(request, true);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expire() {
        requests.expireOpen(EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED), Instant.now());
    }

    private List<DriverProfile> match(RideRequest request) {
        List<DriverProfile> online = drivers.findByOnlineTrueAndVerificationStatus(VerificationStatus.APPROVED).stream()
                .filter(profile -> profile.getLastLat() != null)
                .filter(profile -> within(profile, request))
                .filter(profile -> !profile.getUser().getId().equals(request.getRider().getId()))
                .toList();
        if (!request.isNightOwl()) {
            return online;
        }
        List<DriverProfile> preferred = online.stream()
                .filter(profile -> profile.getUser().getRatingAvg() >= nightOwlMinRating || profile.getUser().getRatingCount() >= 50)
                .sorted(Comparator.comparingDouble((DriverProfile profile) -> profile.getUser().getRatingAvg()).reversed())
                .toList();
        return preferred.isEmpty() ? online : preferred;
    }

    private boolean within(DriverProfile profile, RideRequest request) {
        if (profile.getLastLat() == null || profile.getLastLng() == null) {
            return false;
        }
        return GeoMath.haversineKm(profile.getLastLat(), profile.getLastLng(), request.getPickupLat(), request.getPickupLng()) <= radiusKm;
    }

    private void assertCanView(RideRequest request, User user) {
        boolean rider = request.getRider().getId().equals(user.getId());
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.DRIVER;
        if (!rider && !staff) {
            throw ApiException.forbidden("You cannot view this request");
        }
    }
}
