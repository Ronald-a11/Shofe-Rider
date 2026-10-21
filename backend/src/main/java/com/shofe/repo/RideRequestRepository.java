package com.shofe.repo;

import com.shofe.domain.entity.RideRequest;
import com.shofe.domain.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {
    List<RideRequest> findByRiderIdOrderByCreatedAtDesc(UUID riderId);

    List<RideRequest> findByStatusInOrderByCreatedAtDesc(Collection<RequestStatus> statuses);

    @Modifying
    @Query("""
            update RideRequest r set r.status = com.shofe.domain.enums.RequestStatus.EXPIRED
            where r.status in :open and r.expiresAt < :now
            """)
    int expireOpen(@Param("open") Collection<RequestStatus> open, @Param("now") Instant now);
}
