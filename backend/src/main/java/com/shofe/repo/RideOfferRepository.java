package com.shofe.repo;

import com.shofe.domain.entity.RideOffer;
import com.shofe.domain.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideOfferRepository extends JpaRepository<RideOffer, UUID> {
    List<RideOffer> findByRequestIdOrderByAmountAscCreatedAtAsc(UUID requestId);
    Optional<RideOffer> findByRequestIdAndDriverIdAndStatus(UUID requestId, UUID driverId, OfferStatus status);
    List<RideOffer> findByRequestIdAndStatus(UUID requestId, OfferStatus status);
}
