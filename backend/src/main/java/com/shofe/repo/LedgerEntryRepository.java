package com.shofe.repo;

import com.shofe.domain.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);
    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
