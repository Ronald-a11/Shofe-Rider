package com.shofe.service;

import com.shofe.domain.entity.Ride;
import com.shofe.domain.enums.RideStatus;
import com.shofe.repo.RideRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * During Night Owl trips, prompt the rider for a safety check-in every few minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NightOwlCheckInScheduler {

    private final RideRepository rides;
    private final RideSockets sockets;
    private final NotificationService notifications;

    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void promptCheckIns() {
        Instant now = Instant.now();
        List<Ride> active = rides.findAll().stream()
                .filter(r -> r.getStatus() == RideStatus.IN_PROGRESS && r.isNightOwl())
                .toList();
        for (Ride ride : active) {
            Instant last = ride.getLastCheckInPromptAt();
            if (last != null && Duration.between(last, now).toMinutes() < 5) {
                continue;
            }
            ride.setLastCheckInPromptAt(now);
            notifications.push(ride.getRider(), "Night Owl check-in", "Tap OK if you are safe, or Need help.");
            Map<String, Object> payload = Map.of(
                    "type", "CHECK_IN_PROMPT",
                    "rideId", ride.getId().toString(),
                    "message", "Are you OK?");
            sockets.checkInPrompt(ride.getRider().socketName(), payload);
            log.info("Check-in prompt sent for ride {}", ride.getId());
        }
    }
}
