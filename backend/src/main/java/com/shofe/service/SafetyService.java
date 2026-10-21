package com.shofe.service;

import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.SosEvent;
import com.shofe.domain.entity.User;
import com.shofe.dto.MiscDtos;
import com.shofe.repo.EmergencyContactRepository;
import com.shofe.repo.RideRepository;
import com.shofe.repo.SosEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyService {

    private final CurrentUserService currentUserService;
    private final SosEventRepository events;
    private final RideRepository rides;
    private final EmergencyContactRepository contacts;
    private final NotificationService notifications;

    @Transactional
    public MiscDtos.SosResponse raise(MiscDtos.SosRequest request) {
        User user = currentUserService.require();
        SosEvent event = new SosEvent();
        event.setUser(user);
        event.setLat(request.lat());
        event.setLng(request.lng());
        event.setNote(request.note());
        if (request.rideId() != null) {
            rides.findById(request.rideId()).ifPresent(event::setRide);
        }
        events.save(event);
        contacts.findByUserIdOrderByNameAsc(user.getId()).forEach(contact ->
                log.warn("SOS SMS placeholder -> {} {} from {} at {},{}", contact.getName(), contact.getPhone(),
                        user.getFullName(), request.lat(), request.lng()));
        notifications.push(user, "SOS received", "Shofe logged your emergency alert and notified trusted contacts.");
        Ride ride = event.getRide();
        if (ride != null) {
            log.warn("SOS on ride {} status {}", ride.getId(), ride.getStatus());
        }
        return new MiscDtos.SosResponse(event.getId(), event.getCreatedAt(),
                "Alert sent. Stay on the line with people you trust if you can.");
    }
}
