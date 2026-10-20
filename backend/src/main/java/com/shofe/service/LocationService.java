package com.shofe.service;

import com.shofe.domain.entity.LocationUpdate;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.Role;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.LocationUpdateRepository;
import com.shofe.repo.RideRepository;
import com.shofe.repo.UserRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final CurrentUserService currentUserService;
    private final UserRepository users;
    private final DriverService driverService;
    private final RideRepository rides;
    private final LocationUpdateRepository locations;
    private final RideSockets sockets;

    @Transactional
    public void update(RideDtos.LocationRequest request) {
        updateFor(currentUserService.require(), request);
    }

    @Transactional
    public void updateForEmail(String email, RideDtos.LocationRequest request) {
        User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("User not found"));
        updateFor(user, request);
    }

    private void updateFor(User user, RideDtos.LocationRequest request) {
        if (user.getRole() != Role.DRIVER) {
            throw ApiException.forbidden("Only drivers share live location");
        }
        driverService.updateLocation(user, request.lat(), request.lng(), request.heading());
        Ride ride = null;
        if (request.rideId() != null) {
            ride = rides.findById(request.rideId()).orElse(null);
            if (ride != null && !ride.getDriver().getId().equals(user.getId())) {
                ride = null;
            }
        }
        LocationUpdate update = new LocationUpdate();
        update.setDriver(user);
        update.setRide(ride);
        update.setLat(request.lat());
        update.setLng(request.lng());
        update.setHeading(request.heading());
        locations.save(update);
        UUID rideId = ride == null ? null : ride.getId();
        if (rideId != null) {
            Tx.afterCommit(() -> sockets.location(rideId, new RideDtos.LatLng(request.lat(), request.lng())));
        }
    }
}
