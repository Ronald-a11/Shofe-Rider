package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.RoadsideRequest;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.RoadsideStatus;
import com.shofe.domain.enums.RoadsideType;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.MiscDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.DriverProfileRepository;
import com.shofe.repo.RoadsideRequestRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadsideService {

    private final CurrentUserService currentUserService;
    private final RoadsideRequestRepository roadside;
    private final DriverProfileRepository drivers;
    private final RideSockets sockets;
    private final FareService fareService;

    @Value("${shofe.matching.radius-km}")
    private double radiusKm;

    @Transactional
    public MiscDtos.RoadsideResponse create(MiscDtos.RoadsideCreate body) {
        User rider = currentUserService.require();
        if (!rider.riding()) {
            throw ApiException.badRequest("Switch to passenger mode to request roadside help");
        }
        RoadsideRequest request = new RoadsideRequest();
        request.setRider(rider);
        request.setType(RoadsideType.valueOf(body.type()));
        request.setLat(body.lat());
        request.setLng(body.lng());
        request.setAddress(body.address().trim());
        request.setDetails(body.details());
        request.setFare(body.fare());
        request.setCurrency(fareService.currency());
        roadside.save(request);
        MiscDtos.RoadsideResponse dto = toDto(request);
        List<String> emails = drivers.findByOnlineTrueAndVerificationStatus(VerificationStatus.APPROVED).stream()
                .filter(DriverProfile::isRoadsideOptIn)
                .filter(profile -> profile.getLastLat() != null)
                .filter(profile -> GeoMath.haversineKm(profile.getLastLat(), profile.getLastLng(), body.lat(), body.lng()) <= radiusKm)
                .map(profile -> profile.getUser().socketName())
                .toList();
        Tx.afterCommit(() -> emails.forEach(email -> sockets.roadside(email, dto)));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MiscDtos.RoadsideResponse> mine() {
        User user = currentUserService.require();
        List<RoadsideRequest> rows = user.driving()
                ? roadside.findByProviderIdOrderByCreatedAtDesc(user.getId())
                : roadside.findByRiderIdOrderByCreatedAtDesc(user.getId());
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MiscDtos.RoadsideResponse> open() {
        User user = currentUserService.require();
        if (!user.driving()) {
            throw ApiException.forbidden("Switch to driver mode to see open jobs");
        }
        return roadside.findByStatusInOrderByCreatedAtDesc(EnumSet.of(RoadsideStatus.REQUESTED)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MiscDtos.RoadsideResponse accept(UUID id) {
        User driver = currentUserService.require();
        DriverProfile profile = driver.getDriverProfile();
        if (profile == null || !profile.isRoadsideOptIn() || profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw ApiException.forbidden("Roadside assistance is not enabled on this driver");
        }
        RoadsideRequest request = roadside.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (request.getStatus() != RoadsideStatus.REQUESTED) {
            throw ApiException.conflict("Someone else already took this job");
        }
        request.setProvider(driver);
        request.setStatus(RoadsideStatus.ACCEPTED);
        return publish(request);
    }

    @Transactional
    public MiscDtos.RoadsideResponse status(UUID id, String status) {
        User user = currentUserService.require();
        RoadsideRequest request = roadside.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
        boolean provider = request.getProvider() != null && request.getProvider().getId().equals(user.getId());
        boolean rider = request.getRider().getId().equals(user.getId());
        RoadsideStatus next = RoadsideStatus.valueOf(status);
        if (next == RoadsideStatus.CANCELLED && (rider || provider)) {
            request.setStatus(next);
            return publish(request);
        }
        if (!provider) {
            throw ApiException.forbidden("Only the assigned provider can update this job");
        }
        request.setStatus(next);
        return publish(request);
    }

    private MiscDtos.RoadsideResponse publish(RoadsideRequest request) {
        MiscDtos.RoadsideResponse dto = toDto(request);
        String riderSocket = request.getRider().socketName();
        String providerSocket = request.getProvider() == null ? null : request.getProvider().socketName();
        Tx.afterCommit(() -> {
            sockets.roadside(riderSocket, dto);
            if (providerSocket != null) {
                sockets.roadside(providerSocket, dto);
            }
        });
        return dto;
    }

    private MiscDtos.RoadsideResponse toDto(RoadsideRequest request) {
        DriverProfile profile = request.getProvider() == null ? null : request.getProvider().getDriverProfile();
        return new MiscDtos.RoadsideResponse(
                request.getId(),
                request.getType().name(),
                request.getStatus().name(),
                request.getAddress(),
                request.getDetails(),
                request.getLat(),
                request.getLng(),
                request.getFare(),
                request.getCurrency(),
                request.getRider().getFullName(),
                request.getProvider() == null ? null : request.getProvider().getFullName(),
                profile == null ? null : profile.getLastLat(),
                profile == null ? null : profile.getLastLng(),
                request.getCreatedAt());
    }
}
