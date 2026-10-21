package com.shofe.service;

import com.shofe.domain.entity.ChatMessage;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.ChatMessageRepository;
import com.shofe.repo.RideRepository;
import com.shofe.ws.RideSockets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final CurrentUserService currentUserService;
    private final RideRepository rides;
    private final ChatMessageRepository messages;
    private final RideSockets sockets;

    @Transactional(readOnly = true)
    public List<RideDtos.ChatResponse> list(UUID rideId) {
        Ride ride = participantRide(rideId, currentUserService.require());
        return messages.findByRideIdOrderBySentAtAsc(ride.getId()).stream().map(this::toDto).toList();
    }

    @Transactional
    public RideDtos.ChatResponse send(UUID rideId, String body) {
        User sender = currentUserService.require();
        return sendAs(sender, rideId, body);
    }

    @Transactional
    public RideDtos.ChatResponse sendAs(User sender, UUID rideId, String body) {
        Ride ride = participantRide(rideId, sender);
        ChatMessage message = new ChatMessage();
        message.setRide(ride);
        message.setSender(sender);
        message.setBody(body.trim());
        messages.save(message);
        RideDtos.ChatResponse dto = toDto(message);
        Tx.afterCommit(() -> sockets.chat(ride.getId(), dto));
        return dto;
    }

    private Ride participantRide(UUID rideId, User user) {
        Ride ride = rides.findById(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
        boolean member = ride.getRider().getId().equals(user.getId()) || ride.getDriver().getId().equals(user.getId());
        if (!member) {
            throw ApiException.forbidden("You are not part of this ride");
        }
        return ride;
    }

    private RideDtos.ChatResponse toDto(ChatMessage message) {
        return new RideDtos.ChatResponse(
                message.getId(),
                message.getRide().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getBody(),
                message.getSentAt());
    }
}
