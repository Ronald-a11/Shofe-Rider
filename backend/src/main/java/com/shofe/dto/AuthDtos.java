package com.shofe.dto;

import com.shofe.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 32) String phone,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotNull Role role
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record OtpRequest(
            @NotBlank @Size(max = 4) String countryIso,
            @NotBlank @Size(max = 24) String nationalNumber
    ) {}

    public record OtpVerifyRequest(
            @NotBlank @Size(max = 4) String countryIso,
            @NotBlank @Size(max = 24) String nationalNumber,
            @NotBlank @Size(min = 4, max = 8) String code,
            @Size(max = 120) String fullName
    ) {}

    public record OtpSentResponse(boolean sent, int expiresInSeconds, int retryAfterSeconds, boolean newUser) {}

    public record ModeRequest(@NotBlank String mode) {}

    public record AuthResponse(String token, UserResponse user) {}

    public record DriverSummary(
            UUID id,
            String verificationStatus,
            String vehicleMake,
            String vehicleModel,
            String vehicleColor,
            String plateNumber,
            Integer vehicleYear,
            int tripCount,
            boolean online,
            boolean roadsideOptIn,
            String licenseDocUrl,
            String registrationDocUrl,
            String insuranceDocUrl,
            String rejectionReason,
            Double lastLat,
            Double lastLng
    ) {}

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            String countryIso,
            String role,
            String activeMode,
            boolean phoneVerified,
            double ratingAvg,
            int ratingCount,
            DriverSummary driver
    ) {}

    public record ProfileUpdate(@NotBlank @Size(max = 120) String fullName, @NotBlank @Size(max = 32) String phone) {}

    public record ContactRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 32) String phone) {}

    public record ContactResponse(UUID id, String name, String phone) {}
}
