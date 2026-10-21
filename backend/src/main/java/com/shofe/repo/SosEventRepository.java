package com.shofe.repo;

import com.shofe.domain.entity.SosEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SosEventRepository extends JpaRepository<SosEvent, UUID> {
    List<SosEvent> findTop50ByOrderByCreatedAtDesc();
}
