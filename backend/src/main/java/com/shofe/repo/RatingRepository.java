package com.shofe.repo;

import com.shofe.domain.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByRideIdAndFromUserId(UUID rideId, UUID fromUserId);
}
