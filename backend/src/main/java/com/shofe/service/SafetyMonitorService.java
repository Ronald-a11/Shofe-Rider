package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.enums.RideStatus;
import com.shofe.repo.RideRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PDF Night Owl safety monitors: driver fatigue and route-corridor deviation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyMonitorService {

    private final RideRepository rides;
    private final RideSockets sockets;
    private final NotificationService notifications;

    @Value("${shofe.safety.fatigue-hours:10}")
    private long fatigueHours;

    @Value("${shofe.safety.route-deviation-km:1.5}")
    private double deviationKm;

    @Transactional
    public void onDriverLocation(Ride ride, double lat, double lng) {
        if (ride == null || ride.getStatus() != RideStatus.IN_PROGRESS) {
            return;
        }
        checkRouteDeviation(ride, lat, lng);
        checkFatigue(ride);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> fatigueStatus(DriverProfile profile) {
        Map<String, Object> status = new HashMap<>();
        boolean fatigued = false;
        String reason = null;
        if (profile != null && profile.isOnline() && profile.getOnlineSince() != null) {
            long hours = Duration.between(profile.getOnlineSince(), Instant.now()).toHours();
            status.put("onlineHours", hours);
            if (hours >= fatigueHours) {
                fatigued = true;
                reason = "Online for " + hours + "h — take a break before the next trip";
            }
        }
        status.put("fatigued", fatigued);
        status.put("reason", reason);
        status.put("limitHours", fatigueHours);
        return status;
    }

    private void checkRouteDeviation(Ride ride, double lat, double lng) {
        if (ride.isRouteDeviationAlerted()) {
            return;
        }
        RideRequest request = ride.getRequest();
        double distance = distanceToSegmentKm(
                lat, lng,
                request.getPickupLat(), request.getPickupLng(),
                request.getDestinationLat(), request.getDestinationLng());
        if (distance < deviationKm) {
            return;
        }
        ride.setRouteDeviationAlerted(true);
        rides.save(ride);
        String message = String.format("Route deviation %.1f km from corridor on ride %s", distance, ride.getId());
        log.warn("Night Owl route alert: {}", message);
        notifications.push(ride.getRider(), "Route deviation", "Your driver left the expected route. Stay aware and use SOS if needed.");
        notifications.push(ride.getDriver(), "Route deviation", "You are off the planned corridor. Return to the route or explain in chat.");
        UUID rideId = ride.getId();
        Map<String, Object> payload = Map.of(
                "type", "ROUTE_DEVIATION",
                "rideId", rideId.toString(),
                "distanceKm", Math.round(distance * 10) / 10.0,
                "message", "Driver left the planned corridor");
        Tx.afterCommit(() -> sockets.safetyAlert(rideId, payload));
    }

    private void checkFatigue(Ride ride) {
        DriverProfile profile = ride.getDriver().getDriverProfile();
        if (profile == null || profile.getOnlineSince() == null) {
            return;
        }
        long hours = Duration.between(profile.getOnlineSince(), Instant.now()).toHours();
        if (hours < fatigueHours || ride.isFatigueAlerted()) {
            return;
        }
        ride.setFatigueAlerted(true);
        rides.save(ride);
        notifications.push(ride.getDriver(), "Fatigue alert",
                "You have been online for " + hours + " hours. Rest before accepting more rides.");
        UUID rideId = ride.getId();
        Map<String, Object> payload = Map.of(
                "type", "FATIGUE",
                "rideId", rideId.toString(),
                "onlineHours", hours,
                "message", "Driver fatigue threshold reached");
        Tx.afterCommit(() -> sockets.safetyAlert(rideId, payload));
    }

    /** Distance from point P to segment AB in km. */
    static double distanceToSegmentKm(double plat, double plng, double alat, double alng, double blat, double blng) {
        double ab = GeoMath.haversineKm(alat, alng, blat, blng);
        if (ab < 0.05) {
            return GeoMath.haversineKm(plat, plng, alat, alng);
        }
        double x = Math.toRadians(plng - alng) * Math.cos(Math.toRadians((plat + alat) / 2));
        double y = Math.toRadians(plat - alat);
        double bx = Math.toRadians(blng - alng) * Math.cos(Math.toRadians((blat + alat) / 2));
        double by = Math.toRadians(blat - alat);
        double denom = bx * bx + by * by;
        double t = denom == 0 ? 0 : Math.max(0, Math.min(1, (x * bx + y * by) / denom));
        double clat = alat + t * (blat - alat);
        double clng = alng + t * (blng - alng);
        return GeoMath.haversineKm(plat, plng, clat, clng);
    }
}
