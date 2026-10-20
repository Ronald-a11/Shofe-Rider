package com.shofe.repo;

import com.shofe.domain.entity.RoadsideRequest;
import com.shofe.domain.enums.RoadsideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoadsideRequestRepository extends JpaRepository<RoadsideRequest, UUID> {
    List<RoadsideRequest> findByRiderIdOrderByCreatedAtDesc(UUID riderId);
    List<RoadsideRequest> findByStatusInOrderByCreatedAtDesc(Collection<RoadsideStatus> statuses);
    List<RoadsideRequest> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
