package com.shofe.repo;

import com.shofe.domain.entity.Ride;
import com.shofe.domain.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    Optional<Ride> findByShareToken(String shareToken);
    Optional<Ride> findByRequestId(UUID requestId);

    @Query("""
            select r from Ride r
            where (r.rider.id = :userId or r.driver.id = :userId)
              and r.status in :statuses
            order by r.createdAt desc
            """)
    List<Ride> findActive(UUID userId, Collection<RideStatus> statuses);

    @Query("""
            select r from Ride r
            where r.rider.id = :userId or r.driver.id = :userId
            order by r.createdAt desc
            """)
    List<Ride> history(UUID userId);

    long countByStatus(RideStatus status);
}
