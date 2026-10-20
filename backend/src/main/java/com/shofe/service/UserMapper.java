package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.User;
import com.shofe.dto.AuthDtos;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AuthDtos.UserResponse toUser(User user) {
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getRatingAvg(),
                user.getRatingCount(),
                toDriver(user.getDriverProfile()));
    }

    public AuthDtos.DriverSummary toDriver(DriverProfile profile) {
        if (profile == null) {
            return null;
        }
        return new AuthDtos.DriverSummary(
                profile.getId(),
                profile.getVerificationStatus().name(),
                profile.getVehicleMake(),
                profile.getVehicleModel(),
                profile.getVehicleColor(),
                profile.getPlateNumber(),
                profile.getVehicleYear(),
                profile.getTripCount(),
                profile.isOnline(),
                profile.isRoadsideOptIn(),
                profile.getLicenseDocUrl(),
                profile.getRegistrationDocUrl(),
                profile.getInsuranceDocUrl(),
                profile.getRejectionReason(),
                profile.getLastLat(),
                profile.getLastLng());
    }
}
