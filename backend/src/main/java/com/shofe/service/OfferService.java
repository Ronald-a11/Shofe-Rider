package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.RideOffer;
import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.OfferStatus;
import com.shofe.domain.enums.OfferType;
import com.shofe.domain.enums.RequestStatus;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.RideOfferRepository;
import com.shofe.repo.RideRequestRepository;
import com.shofe.repo.UserRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final CurrentUserService currentUserService;
    private final RideRequestRepository requests;
    private final RideOfferRepository offers;
    private final UserRepository users;
    private final RideMapper rideMapper;
    private final RideSockets sockets;
    private final NotificationService notifications;

    @Transactional
    public RideDtos.OfferResponse submit(UUID requestId, RideDtos.OfferRequest body) {
        User driver = currentUserService.require();
        if (!driver.driving()) {
            throw ApiException.forbidden("Switch to driver mode to send offers");
        }
        OfferType type = OfferType.valueOf(body.type());
        return create(driver, loadOpen(requestId), body.amount(), type, body.etaMinutes(), body.message());
    }

    @Transactional
    public void systemOffer(UUID driverId, UUID requestId, boolean accept, int eta) {
        RideRequest request = requests.findById(requestId).orElse(null);
        if (request == null || !EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED).contains(request.getStatus())) {
            return;
        }
        User driver = users.findById(driverId).orElse(null);
        if (driver == null) {
            return;
        }
        BigDecimal amount = accept
                ? request.getProposedFare()
                : request.getProposedFare().add(new BigDecimal("15")).setScale(0, RoundingMode.HALF_UP);
        String message = accept ? "I can do your fare" : "Small bump for this route";
        create(driver, request, amount, accept ? OfferType.ACCEPT : OfferType.COUNTER, eta, message);
    }

    @Transactional(readOnly = true)
    public List<RideDtos.OfferResponse> list(UUID requestId) {
        RideRequest request = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        User user = currentUserService.require();
        if (!request.getRider().getId().equals(user.getId()) && !user.driving()) {
            throw ApiException.forbidden("You cannot view these offers");
        }
        return offers.findByRequestIdOrderByAmountAscCreatedAtAsc(requestId).stream().map(rideMapper::toOffer).toList();
    }

    private RideDtos.OfferResponse create(User driver, RideRequest request, BigDecimal amount, OfferType type, int eta, String message) {
        DriverProfile profile = driver.getDriverProfile();
        if (profile == null || profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw ApiException.forbidden("Driver is not approved");
        }
        if (request.getRider().getId().equals(driver.getId())) {
            throw ApiException.badRequest("You cannot bid on your own request");
        }
        if (type == OfferType.ACCEPT && amount.compareTo(request.getProposedFare()) != 0) {
            amount = request.getProposedFare();
        }
        BigDecimal min = request.getProposedFare().multiply(new BigDecimal("0.5"));
        BigDecimal max = request.getProposedFare().multiply(new BigDecimal("2"));
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            throw ApiException.badRequest("Offer must stay between 50% and 200% of the proposed fare");
        }
        RideOffer offer = offers.findByRequestIdAndDriverIdAndStatus(request.getId(), driver.getId(), OfferStatus.PENDING)
                .orElseGet(RideOffer::new);
        offer.setRequest(request);
        offer.setDriver(driver);
        offer.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        offer.setType(type);
        offer.setEtaMinutes(eta);
        offer.setStatus(OfferStatus.PENDING);
        offer.setMessage(message);
        offers.save(offer);
        if (request.getStatus() == RequestStatus.REQUESTED) {
            request.setStatus(RequestStatus.OFFERS_RECEIVED);
        }
        RideDtos.OfferResponse dto = rideMapper.toOffer(offer);
        Tx.afterCommit(() -> {
            sockets.offerToRider(request.getRider().socketName(), dto);
            notifications.push(request.getRider(), "New offer", driver.getFullName() + " offered " + dto.amount());
        });
        return dto;
    }

    private RideRequest loadOpen(UUID id) {
        RideRequest request = requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (!EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED).contains(request.getStatus())) {
            throw ApiException.conflict("This request is no longer open");
        }
        if (request.getExpiresAt().isBefore(java.time.Instant.now())) {
            request.setStatus(RequestStatus.EXPIRED);
            throw ApiException.conflict("This request has expired");
        }
        return request;
    }
}
