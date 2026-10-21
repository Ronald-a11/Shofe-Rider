package com.shofe.repo;

import com.shofe.domain.entity.NightCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NightCheckInRepository extends JpaRepository<NightCheckIn, UUID> {
    List<NightCheckIn> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
