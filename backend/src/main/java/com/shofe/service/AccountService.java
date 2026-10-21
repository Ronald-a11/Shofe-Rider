package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.EmergencyContact;
import com.shofe.domain.entity.Rating;
import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.ActiveMode;
import com.shofe.domain.enums.RideStatus;
import com.shofe.domain.enums.Role;
import com.shofe.dto.AuthDtos;
import com.shofe.dto.RideDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.EmergencyContactRepository;
import com.shofe.repo.RatingRepository;
import com.shofe.repo.RideRepository;
import com.shofe.repo.UserRepository;
import com.shofe.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CurrentUserService currentUserService;
    private final UserRepository users;
    private final EmergencyContactRepository contacts;
    private final RideRepository rides;
    private final RatingRepository ratings;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final WalletService walletService;

    @Transactional
    public AuthDtos.UserResponse updateProfile(AuthDtos.ProfileUpdate request) {
        User user = currentUserService.require();
        user.setFullName(request.fullName().trim());
        return userMapper.toUser(user);
    }

    @Transactional
    public AuthDtos.AuthResponse switchMode(AuthDtos.ModeRequest request) {
        User user = currentUserService.require();
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts stay on the operations desk");
        }
        ActiveMode next;
        try {
            next = ActiveMode.valueOf(request.mode().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Choose passenger or driver mode");
        }
        if (!rides.findActive(user.getId(), EnumSet.of(
                RideStatus.ACCEPTED, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS)).isEmpty()) {
            throw ApiException.badRequest("Finish or cancel your current trip before switching modes");
        }
        if (next == ActiveMode.DRIVER) {
            if (user.getDriverProfile() == null) {
                DriverProfile profile = new DriverProfile();
                profile.setUser(user);
                user.setDriverProfile(profile);
            }
            user.setRole(Role.DRIVER);
            user.setActiveMode(ActiveMode.DRIVER);
            walletService.requireWallet(user);
        } else {
            DriverProfile profile = user.getDriverProfile();
            if (profile != null) {
                profile.setOnline(false);
                profile.setOnlineSince(null);
            }
            user.setActiveMode(ActiveMode.PASSENGER);
        }
        users.save(user);
        return new AuthDtos.AuthResponse(jwtService.issue(user), userMapper.toUser(user));
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.ContactResponse> contacts() {
        return contacts.findByUserIdOrderByNameAsc(currentUserService.require().getId()).stream()
                .map(contact -> new AuthDtos.ContactResponse(contact.getId(), contact.getName(), contact.getPhone()))
                .toList();
    }

    @Transactional
    public AuthDtos.ContactResponse addContact(AuthDtos.ContactRequest request) {
        User user = currentUserService.require();
        if (contacts.findByUserIdOrderByNameAsc(user.getId()).size() >= 5) {
            throw ApiException.badRequest("You can save up to 5 trusted contacts");
        }
        EmergencyContact contact = new EmergencyContact();
        contact.setUser(user);
        contact.setName(request.name().trim());
        contact.setPhone(request.phone().trim());
        contacts.save(contact);
        return new AuthDtos.ContactResponse(contact.getId(), contact.getName(), contact.getPhone());
    }

    @Transactional
    public void deleteContact(UUID id) {
        User user = currentUserService.require();
        EmergencyContact contact = contacts.findById(id).orElseThrow(() -> ApiException.notFound("Contact not found"));
        if (!contact.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("That contact is not yours");
        }
        contacts.delete(contact);
    }

    @Transactional
    public void rate(UUID rideId, RideDtos.RateRequest request) {
        User from = currentUserService.require();
        Ride ride = rides.findById(rideId).orElseThrow(() -> ApiException.notFound("Ride not found"));
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw ApiException.badRequest("You can rate a ride after it is completed");
        }
        boolean rider = ride.getRider().getId().equals(from.getId());
        boolean driver = ride.getDriver().getId().equals(from.getId());
        if (!rider && !driver) {
            throw ApiException.forbidden("You were not on this ride");
        }
        if (ratings.findByRideIdAndFromUserId(rideId, from.getId()).isPresent()) {
            throw ApiException.conflict("You already rated this ride");
        }
        User to = rider ? ride.getDriver() : ride.getRider();
        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setFromUser(from);
        rating.setToUser(to);
        rating.setScore(request.score());
        rating.setComment(request.comment());
        ratings.save(rating);
        int count = to.getRatingCount();
        double next = count == 0 ? request.score() : ((to.getRatingAvg() * count) + request.score()) / (count + 1);
        to.setRatingAvg(Math.round(next * 10.0) / 10.0);
        to.setRatingCount(count + 1);
        users.save(to);
    }
}
