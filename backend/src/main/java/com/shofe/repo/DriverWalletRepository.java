package com.shofe.repo;

import com.shofe.domain.entity.DriverWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverWalletRepository extends JpaRepository<DriverWallet, UUID> {
    Optional<DriverWallet> findByUserId(UUID userId);
}
