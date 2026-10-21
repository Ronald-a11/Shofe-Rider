package com.shofe.repo;

import com.shofe.domain.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, UUID> {
}
