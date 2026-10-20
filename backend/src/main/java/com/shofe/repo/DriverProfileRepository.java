package com.shofe.repo;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUserId(UUID userId);
    List<DriverProfile> findByOnlineTrueAndVerificationStatus(VerificationStatus status);
    List<DriverProfile> findByVerificationStatusOrderByUser_FullNameAsc(VerificationStatus status);
}
