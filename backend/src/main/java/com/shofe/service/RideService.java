package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.Payment;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.RideOffer;
import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.OfferStatus;
import com.shofe.domain.enums.PaymentMethod;
import com.shofe.domain.enums.PaymentStatus;
import com.shofe.domain.enums.RequestStatus;
import com.shofe.domain.enums.RideStatus;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.EmergencyContactRepository;
import com.shofe.repo.PaymentRepository;
import com.shofe.repo.RideOfferRepository;
import com.shofe.repo.RideRepository;
import com.shofe.repo.RideRequestRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RideService {

    private static final Set<RideStatus> ACTIVE = EnumSet.of(RideStatus.ACCEPTED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS);

    private final CurrentUserService currentUserService;
    private final RideRepository rides;
    private final RideRequestRepository requests;
    private final RideOfferRepository offers;
    private final PaymentRepository payments;
    private final EmergencyContactRepository contacts;
    private final RideMapper rideMapper;
    private final RideSockets sockets;
    private final NotificationService notifications;
    private final WalletService walletService;

    @Transactional
    public RideDtos.RideResponse selectOffer(UUID offerId) {
        User rider = currentUserService.require();
        RideOffer offer = offers.findById(offerId).orElseThrow(() -> ApiException.notFound("Offer not found"));
        RideRequest request = offer.getRequest();
        if (!request.getRider().getId().equals(rider.getId())) {
            throw ApiException.forbidden("Only the rider can choose a driver");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw ApiException.conflict("That offer is no longer available");
        }
        if (!EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED).contains(request.getStatus())) {
            throw ApiException.conflict("This request is no longer open");
        }
        offer.setStatus(OfferStatus.SELECTED);
        for (RideOffer other : offers.findByRequestIdAndStatus(request.getId(), OfferStatus.PENDING)) {
            other.setStatus(OfferStatus.DECLINED);
        }
        request.setStatus(RequestStatus.ACCEPTED);

        Ride ride = new Ride();
        ride.setRequest(request);
        ride.setRider(rider);
        ride.setDriver(offer.getDriver());
        ride.setOffer(offer);
        ride.setFare(offer.getAmount());
        ride.setCurrency(request.getCurrency());
        ride.setNightOwl(request.isNightOwl());
        ride.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        if (request.isNightOwl()) {
            ride.setPinCode(String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000)));
        }
        rides.save(ride);

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(ride.getFare());
        payment.setCurrency(ride.getCurrency());
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus(request.getPaymentMethod() == PaymentMethod.CASH
                ? PaymentStatus.PENDING
                : PaymentStatus.AWAITING_PAYMENT);
        payments.save(payment);

        RideDtos.RideResponse dto = rideMapper.toRide(ride, rider);
        RideDtos.RideResponse forDriver = rideMapper.toRide(ride, ride.getDriver());
        var sharedWith = ride.isNightOwl() ? contacts.findByUserIdOrderByNameAsc(rider.getId()) : java.util.List.<com.shofe.domain.entity.EmergencyContact>of();
        Tx.afterCommit(() -> {
            sockets.rideUpdate(rider.getEmail(), dto);
            sockets.rideUpdate(ride.getDriver().getEmail(), forDriver);
            notifications.push(ride.getDriver(), "Offer accepted", rider.getFullName() + " chose you");
            if (ride.isNightOwl()) {
                notifications.shareRide(rider, ride, sharedWith);
            }
        });
        return dto;
    }

    @Transactional(readOnly = true)
    public RideDtos.RideResponse get(UUID id) {
        User user = currentUserService.require();
        Ride ride = loadParticipant(id, user);
        return rideMapper.toRide(ride, user);
    }

    @Transactional(readOnly = true)
    public RideDtos.RideResponse active() {
        User user = currentUserService.require();
        return rides.findActive(user.getId(), ACTIVE).stream().findFirst()
                .map(ride -> rideMapper.toRide(ride, user))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> history() {
        User user = currentUserService.require();
        return rides.history(user.getId()).stream().map(ride -> rideMapper.toRide(ride, user)).toList();
    }

    @Transactional
    public RideDtos.RideResponse updateStatus(UUID id, RideDtos.StatusRequest body) {
        User user = currentUserService.require();
        Ride ride = loadParticipant(id, user);
        RideStatus next = RideStatus.valueOf(body.status());
        if (next == RideStatus.CANCELLED) {
            return cancel(ride, user, body.reason());
        }
        if (!ride.getDriver().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the driver can advance the ride");
        }
        if (!allowed(ride.getStatus(), next)) {
            throw ApiException.conflict("Cannot move from " + ride.getStatus() + " to " + next);
        }
        if (next == RideStatus.IN_PROGRESS && ride.isNightOwl() && !ride.isPinVerified()) {
            throw ApiException.badRequest("Enter the rider's Night Owl PIN before starting");
        }
        ride.setStatus(next);
        if (next == RideStatus.IN_PROGRESS) {
            ride.setStartedAt(Instant.now());
        }
        if (next == RideStatus.COMPLETED) {
            finish(ride);
        }
        return publish(ride, user);
    }

    @Transactional
    public RideDtos.RideResponse verifyPin(UUID id, String pin) {
        User user = currentUserService.require();
        Ride ride = loadParticipant(id, user);
        if (!ride.getDriver().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the driver verifies the PIN");
        }
        if (!ride.isNightOwl()) {
            throw ApiException.badRequest("This ride does not use a PIN");
        }
        if (ride.getPinCode() == null || !ride.getPinCode().equals(pin)) {
            throw ApiException.badRequest("That PIN does not match");
        }
        ride.setPinVerified(true);
        return publish(ride, user);
    }

    @Transactional
    public RideDtos.RideResponse pay(UUID id) {
        User user = currentUserService.require();
        Ride ride = loadParticipant(id, user);
        if (!ride.getRider().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the rider can pay");
        }
        Payment payment = payments.findFirstByRideIdOrderByCreatedAtDesc(ride.getId())
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (payment.getMethod() == PaymentMethod.CASH) {
            throw ApiException.badRequest("Cash is settled with the driver");
        }
        // Legacy confirm for Stripe stub without redirect; Pesepay uses /api/payments/rides/{id}/start
        payment.setStatus(PaymentStatus.PAID);
        if (payment.getProviderRef() == null) {
            payment.setProviderRef("pi_stub_" + UUID.randomUUID().toString().substring(0, 8));
        }
        return rideMapper.toRide(ride, user);
    }

    @Transactional(readOnly = true)
    public RideDtos.TrackResponse track(String token) {
        Ride ride = rides.findByShareToken(token).orElseThrow(() -> ApiException.notFound("Shared ride not found"));
        return rideMapper.toTrack(ride);
    }

    private RideDtos.RideResponse cancel(Ride ride, User user, String reason) {
        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw ApiException.conflict("Completed rides cannot be cancelled");
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelReason(reason == null || reason.isBlank() ? "Cancelled" : reason.trim());
        ride.getRequest().setStatus(RequestStatus.CANCELLED);
        return publish(ride, user);
    }

    private void finish(Ride ride) {
        ride.setCompletedAt(Instant.now());
        DriverProfile profile = ride.getDriver().getDriverProfile();
        if (profile != null) {
            profile.setTripCount(profile.getTripCount() + 1);
        }
        payments.findFirstByRideIdOrderByCreatedAtDesc(ride.getId()).ifPresent(payment -> {
            if (payment.getMethod() == PaymentMethod.CASH) {
                payment.setStatus(PaymentStatus.PAID);
            }
        });
        walletService.deductCommission(ride);
    }

    private boolean allowed(RideStatus from, RideStatus to) {
        return switch (from) {
            case ACCEPTED -> to == RideStatus.DRIVER_ARRIVED;
            case DRIVER_ARRIVED -> to == RideStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == RideStatus.COMPLETED;
            default -> false;
        };
    }

    private Ride loadParticipant(UUID id, User user) {
        Ride ride = rides.findById(id).orElseThrow(() -> ApiException.notFound("Ride not found"));
        boolean member = ride.getRider().getId().equals(user.getId()) || ride.getDriver().getId().equals(user.getId());
        if (!member && user.getRole() != com.shofe.domain.enums.Role.ADMIN) {
            throw ApiException.forbidden("You are not part of this ride");
        }
        return ride;
    }

    private RideDtos.RideResponse publish(Ride ride, User viewer) {
        RideDtos.RideResponse forRider = rideMapper.toRide(ride, ride.getRider());
        RideDtos.RideResponse forDriver = rideMapper.toRide(ride, ride.getDriver());
        Tx.afterCommit(() -> {
            sockets.rideUpdate(ride.getRider().getEmail(), forRider);
            sockets.rideUpdate(ride.getDriver().getEmail(), forDriver);
        });
        return ride.getRider().getId().equals(viewer.getId()) ? forRider : forDriver;
    }

}
