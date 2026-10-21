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

    @Value("${shofe.matching.radius-km}")
    private double radiusKm;
    @Value("${shofe.matching.night-owl-min-rating}")
    private double nightOwlMinRating;
    @Value("${shofe.matching.request-ttl-minutes}")
    private long ttlMinutes;
    @Value("${shofe.fare.minimum}")
    private BigDecimal minimumFare;

    @Transactional
    public RideDtos.RideRequestResponse create(RideDtos.CreateRequest body) {
        User rider = currentUserService.require();
        if (!rider.riding()) {
            throw ApiException.badRequest("Switch to passenger mode to request a trip");
        }
        RideDtos.QuoteResponse quote = fareService.quote(new RideDtos.QuoteRequest(
                body.pickupLat(), body.pickupLng(), body.destinationLat(), body.destinationLng(), body.nightOwl()));
        if (body.proposedFare().compareTo(minimumFare) < 0) {
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
        List<String> socketsTo = matched.stream().map(profile -> profile.getUser().socketName()).toList();
        for (DriverProfile driver : matched) {
            notifications.push(driver.getUser(), "New Shofe request",
                    request.getPickupAddress() + " → " + request.getDestinationAddress());
        }
        Tx.afterCommit(() -> {
            sockets.requestBroadcast(dto);
            for (String name : socketsTo) {
                sockets.requestToDriver(name, dto);
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
        if (!user.driving()) {
            return List.of();
        }
        DriverProfile profile = user.getDriverProfile();
        if (profile == null || !profile.isOnline()) {
            return List.of();
        }
        return requests.findByStatusInOrderByCreatedAtDesc(EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED))
                .stream()
                .filter(request -> !request.getRider().getId().equals(user.getId()))
                .filter(request -> !request.isNightOwl() || user.getRatingAvg() >= nightOwlMinRating || user.getRatingCount() == 0)
                .sorted(Comparator.comparingDouble(request -> distanceKm(profile, request)))
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
                .filter(profile -> !profile.getUser().getId().equals(request.getRider().getId()))
                .toList();
        List<DriverProfile> nearby = online.stream()
                .filter(profile -> within(profile, request))
                .toList();
        List<DriverProfile> pool = nearby.isEmpty() ? online : nearby;
        if (!request.isNightOwl()) {
            return pool;
        }
        List<DriverProfile> preferred = pool.stream()
                .filter(profile -> profile.getUser().getRatingAvg() >= nightOwlMinRating || profile.getUser().getRatingCount() >= 50)
                .sorted(Comparator.comparingDouble((DriverProfile profile) -> profile.getUser().getRatingAvg()).reversed())
                .toList();
        return preferred.isEmpty() ? pool : preferred;
    }

    private boolean within(DriverProfile profile, RideRequest request) {
        return distanceKm(profile, request) <= radiusKm;
    }

    private double distanceKm(DriverProfile profile, RideRequest request) {
        double lat = profile.getLastLat() == null ? -17.8292 : profile.getLastLat();
        double lng = profile.getLastLng() == null ? 31.0522 : profile.getLastLng();
        return GeoMath.haversineKm(lat, lng, request.getPickupLat(), request.getPickupLng());
    }

    private void assertCanView(RideRequest request, User user) {
        boolean rider = request.getRider().getId().equals(user.getId());
        boolean staff = user.getRole() == Role.ADMIN || user.driving();
        if (!rider && !staff) {
            throw ApiException.forbidden("You cannot view this request");
        }
    }
}
