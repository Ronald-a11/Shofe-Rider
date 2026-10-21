package com.shofe.service;

import com.shofe.domain.entity.EmergencyContact;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Placeholder for Firebase Cloud Messaging and SMS.
 * Logs the payload so the integration point is real and easy to replace.
 */
@Slf4j
@Service
public class NotificationService {

    public void push(User user, String title, String body) {
        log.info("FCM placeholder -> user {} : {} — {}", user.getId(), title, body);
    }

    public void shareRide(User rider, Ride ride, List<EmergencyContact> contacts) {
        for (EmergencyContact contact : contacts) {
            log.info("SMS placeholder -> {} {}: Night Owl ride {} shared. Track token {}",
                    contact.getName(), contact.getPhone(), ride.getId(), ride.getShareToken());
        }
        if (contacts.isEmpty()) {
            log.info("Night Owl ride {} has no emergency contacts to notify", ride.getId());
        }
    }
}
