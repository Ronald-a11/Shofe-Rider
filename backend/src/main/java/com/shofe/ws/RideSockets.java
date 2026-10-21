package com.shofe.ws;

import com.shofe.dto.RideDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RideSockets {

    private final SimpMessagingTemplate broker;

    public void requestToDriver(String driverSocket, RideDtos.RideRequestResponse payload) {
        broker.convertAndSendToUser(driverSocket, "/queue/requests", payload);
    }

    public void requestBroadcast(RideDtos.RideRequestResponse payload) {
        broker.convertAndSend("/topic/requests", payload);
    }

    public void offerToRider(String riderEmail, RideDtos.OfferResponse payload) {
        broker.convertAndSendToUser(riderEmail, "/queue/offers", payload);
    }

    public void rideUpdate(String email, RideDtos.RideResponse payload) {
        broker.convertAndSendToUser(email, "/queue/rides", payload);
        broker.convertAndSend("/topic/rides/" + payload.id(), payload);
    }

    public void chat(UUID rideId, RideDtos.ChatResponse payload) {
        broker.convertAndSend("/topic/rides/" + rideId + "/chat", payload);
    }

    public void location(UUID rideId, RideDtos.LatLng payload) {
        broker.convertAndSend("/topic/rides/" + rideId + "/location", payload);
    }

    public void roadside(String email, Object payload) {
        broker.convertAndSendToUser(email, "/queue/roadside", payload);
    }

    public void safetyAlert(UUID rideId, Object payload) {
        broker.convertAndSend("/topic/rides/" + rideId + "/safety", payload);
    }

    public void checkInPrompt(String email, Object payload) {
        broker.convertAndSendToUser(email, "/queue/check-in", payload);
    }
}
