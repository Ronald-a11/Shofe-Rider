package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.repo.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DemoOfferSimulator {

    private final DriverProfileRepository drivers;
    private final OfferService offerService;
    private final TaskScheduler taskScheduler;

    @Value("${shofe.dev-tools}")
    private boolean devTools;

    public void schedule(UUID requestId) {
        if (!devTools) {
            return;
        }
        List<DriverProfile> bots = drivers.findByOnlineTrueAndVerificationStatus(VerificationStatus.APPROVED).stream()
                .filter(DriverProfile::isDemoBot)
                .toList();
        int delay = 1200;
        int index = 0;
        for (DriverProfile bot : bots) {
            boolean accept = index == 0;
            int eta = 4 + (index * 3);
            int wait = delay;
            UUID driverId = bot.getUser().getId();
            taskScheduler.schedule(
                    () -> offerService.systemOffer(driverId, requestId, accept, eta),
                    Instant.now().plusMillis(wait));
            delay += 1600;
            index++;
        }
    }
}
