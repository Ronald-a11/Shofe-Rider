package com.shofe.ws;

import com.shofe.dto.RideDtos;
import com.shofe.repo.UserRepository;
import com.shofe.service.ChatService;
import com.shofe.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StompController {

    private final LocationService locationService;
    private final ChatService chatService;
    private final UserRepository users;

    @MessageMapping("/location")
    public void location(RideDtos.LocationRequest request, Principal principal) {
        if (principal != null) {
            locationService.updateForEmail(principal.getName(), request);
        }
    }

    public record ChatIn(UUID rideId, String body) {}

    @MessageMapping("/chat")
    public void chat(ChatIn message, Principal principal) {
        if (principal == null || message.body() == null || message.body().isBlank() || message.rideId() == null) {
            return;
        }
        users.findByEmailIgnoreCase(principal.getName())
                .ifPresent(user -> chatService.sendAs(user, message.rideId(), message.body()));
    }
}
