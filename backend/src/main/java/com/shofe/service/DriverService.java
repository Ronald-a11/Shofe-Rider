package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.AuthDtos;
import com.shofe.dto.MiscDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.DriverProfileRepository;
import com.shofe.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final CurrentUserService currentUserService;
    private final DriverProfileRepository drivers;
    private final UserRepository users;
    private final FileStorageService storage;
    private final UserMapper userMapper;
    private final WalletService walletService;

    @Transactional
    public AuthDtos.UserResponse saveProfile(MiscDtos.DriverProfileRequest request,
                                             MultipartFile license,
                                             MultipartFile registration,
                                             MultipartFile insurance) {
        User user = requireDriver(currentUserService.require());
        DriverProfile profile = user.getDriverProfile();
        if (profile == null) {
            profile = new DriverProfile();
            profile.setUser(user);
            user.setDriverProfile(profile);
        }
        profile.setLicenseNumber(request.licenseNumber().trim());
        profile.setVehicleMake(request.vehicleMake().trim());
        profile.setVehicleModel(request.vehicleModel().trim());
        profile.setVehicleColor(request.vehicleColor().trim());
        profile.setPlateNumber(request.plateNumber().trim().toUpperCase());
        profile.setVehicleYear(request.vehicleYear());
        profile.setRoadsideOptIn(request.roadsideOptIn());
        String licenseUrl = storage.store(license);
        String registrationUrl = storage.store(registration);
        String insuranceUrl = storage.store(insurance);
        if (licenseUrl != null) profile.setLicenseDocUrl(licenseUrl);
        if (registrationUrl != null) profile.setRegistrationDocUrl(registrationUrl);
        if (insuranceUrl != null) profile.setInsuranceDocUrl(insuranceUrl);
        if (profile.getLicenseDocUrl() == null || profile.getRegistrationDocUrl() == null || profile.getInsuranceDocUrl() == null) {
            throw ApiException.badRequest("License, vehicle registration, and insurance documents are required");
        }
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setRejectionReason(null);
        walletService.requireWallet(user);
        users.save(user);
        return userMapper.toUser(user);
    }

    @Transactional
    public AuthDtos.UserResponse goOnline(MiscDtos.OnlineRequest request) {
        User user = requireDriver(currentUserService.require());
        DriverProfile profile = approved(user);
        walletService.requireWallet(user);
        profile.setOnline(true);
        if (profile.getOnlineSince() == null) {
            profile.setOnlineSince(java.time.Instant.now());
        }
        profile.setRoadsideOptIn(request.roadsideOptIn());
        touch(profile, request.lat(), request.lng(), null);
        return userMapper.toUser(user);
    }

    @Transactional
    public AuthDtos.UserResponse goOffline() {
        User user = requireDriver(currentUserService.require());
        DriverProfile profile = user.getDriverProfile();
        if (profile != null) {
            profile.setOnline(false);
            profile.setOnlineSince(null);
        }
        return userMapper.toUser(user);
    }

    @Transactional
    public void updateLocation(User driver, double lat, double lng, Double heading) {
        DriverProfile profile = driver.getDriverProfile();
        if (profile == null || !profile.isOnline()) {
            return;
        }
        touch(profile, lat, lng, heading);
    }

    @Transactional
    public AuthDtos.DriverSummary verify(UUID userId, MiscDtos.VerificationRequest request) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Driver not found"));
        DriverProfile profile = user.getDriverProfile();
        if (profile == null) {
            throw ApiException.badRequest("That user has no driver profile");
        }
        VerificationStatus status = VerificationStatus.valueOf(request.status());
        if (status == VerificationStatus.PENDING) {
            throw ApiException.badRequest("Choose APPROVED or REJECTED");
        }
        profile.setVerificationStatus(status);
        profile.setRejectionReason(status == VerificationStatus.REJECTED ? request.reason() : null);
        if (status == VerificationStatus.REJECTED) {
            profile.setOnline(false);
        }
        return userMapper.toDriver(profile);
    }

    private void touch(DriverProfile profile, double lat, double lng, Double heading) {
        profile.setLastLat(lat);
        profile.setLastLng(lng);
        profile.setHeading(heading);
        profile.setLastLocationAt(Instant.now());
        drivers.save(profile);
    }

    private DriverProfile approved(User user) {
        DriverProfile profile = user.getDriverProfile();
        if (profile == null || profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw ApiException.forbidden("Your driver documents are not approved yet");
        }
        return profile;
    }

    private User requireDriver(User user) {
        if (!user.driving()) {
            throw ApiException.forbidden("Switch to driver mode first");
        }
        return user;
    }
}
