package com.shofe.service;

import com.shofe.domain.entity.PhoneOtp;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.ActiveMode;
import com.shofe.domain.enums.Role;
import com.shofe.dto.AuthDtos;
import com.shofe.exception.ApiException;
import com.shofe.integrations.sms.SmsGateway;
import com.shofe.repo.PhoneOtpRepository;
import com.shofe.repo.UserRepository;
import com.shofe.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneOtpRepository otps;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SmsGateway smsGateway;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${shofe.otp.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${shofe.otp.resend-seconds:45}")
    private long resendSeconds;

    @Value("${shofe.otp.max-sends:5}")
    private int maxSends;

    @Value("${shofe.otp.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public AuthDtos.OtpSentResponse request(AuthDtos.OtpRequest request) {
        PhoneNumbers.Country country = PhoneNumbers.requireCountry(request.countryIso());
        String phone = PhoneNumbers.e164(country.iso(), request.nationalNumber());
        Instant now = Instant.now();
        PhoneOtp row = otps.findByPhone(phone).orElseGet(PhoneOtp::new);
        if (row.getId() != null && row.getLastSentAt() != null
                && Duration.between(row.getLastSentAt(), now).getSeconds() < resendSeconds) {
            throw ApiException.tooMany("Wait a moment before requesting another code");
        }
        if (row.getLastSentAt() != null
                && Duration.between(row.getLastSentAt(), now).toHours() < 1
                && row.getSendCount() >= maxSends) {
            throw ApiException.tooMany("Too many codes sent to this number. Try later.");
        }
        if (row.getId() == null || row.getLastSentAt() == null
                || Duration.between(row.getLastSentAt(), now).toHours() >= 1) {
            row.setSendCount(0);
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        row.setPhone(phone);
        row.setCodeHash(passwordEncoder.encode(code));
        row.setExpiresAt(now.plusSeconds(ttlSeconds));
        row.setLastSentAt(now);
        row.setSendCount(row.getSendCount() + 1);
        row.setAttemptCount(0);
        otps.save(row);

        smsGateway.send(phone, "Your Shofe code is " + code + ". It expires in " + (ttlSeconds / 60) + " minutes.");
        boolean newUser = users.findByPhone(phone).isEmpty();
        return new AuthDtos.OtpSentResponse(
                true,
                (int) ttlSeconds,
                (int) resendSeconds,
                newUser);
    }

    @Transactional
    public AuthDtos.AuthResponse verify(AuthDtos.OtpVerifyRequest request) {
        PhoneNumbers.Country country = PhoneNumbers.requireCountry(request.countryIso());
        String phone = PhoneNumbers.e164(country.iso(), request.nationalNumber());
        PhoneOtp row = otps.findByPhone(phone).orElseThrow(() -> ApiException.badRequest("Request a code first"));
        Instant now = Instant.now();
        if (row.getExpiresAt().isBefore(now)) {
            otps.delete(row);
            throw ApiException.badRequest("That code has expired. Request a new one.");
        }
        if (row.getAttemptCount() >= maxAttempts) {
            otps.delete(row);
            throw ApiException.tooMany("Too many attempts. Request a new code.");
        }
        String code = request.code() == null ? "" : request.code().trim();
        if (!passwordEncoder.matches(code, row.getCodeHash())) {
            row.setAttemptCount(row.getAttemptCount() + 1);
            throw ApiException.badRequest("That code is not correct");
        }
        otps.delete(row);

        User user = users.findByPhone(phone).orElse(null);
        if (user == null) {
            String name = request.fullName() == null ? "" : request.fullName().trim();
            if (!StringUtils.hasText(name)) {
                throw ApiException.badRequest("Enter your name to finish registration");
            }
            user = new User();
            user.setFullName(name);
            user.setPhone(phone);
            user.setCountryIso(country.iso());
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(Role.RIDER);
            user.setActiveMode(ActiveMode.PASSENGER);
            user.setPhoneVerified(true);
            users.save(user);
        } else {
            user.setPhoneVerified(true);
            user.setCountryIso(country.iso());
            if (StringUtils.hasText(request.fullName()) && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(request.fullName().trim());
            }
        }
        return new AuthDtos.AuthResponse(jwtService.issue(user), userMapper.toUser(user));
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void sweepExpired() {
        otps.deleteByExpiresAtBefore(Instant.now());
    }
}
