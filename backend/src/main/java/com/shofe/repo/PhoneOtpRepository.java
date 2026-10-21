package com.shofe.repo;

import com.shofe.domain.entity.PhoneOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PhoneOtpRepository extends JpaRepository<PhoneOtp, UUID> {
    Optional<PhoneOtp> findByPhone(String phone);
    void deleteByExpiresAtBefore(Instant cutoff);
}
