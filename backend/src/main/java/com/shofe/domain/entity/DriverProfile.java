package com.shofe.domain.entity;

import com.shofe.domain.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String licenseNumber;
    private String licenseDocUrl;
    private String registrationDocUrl;
    private String insuranceDocUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    private String vehicleMake;
    private String vehicleModel;
    private String vehicleColor;
    private String plateNumber;
    private Integer vehicleYear;

    @Column(nullable = false)
    private int tripCount = 0;

    @Column(nullable = false)
    private boolean online;

    @Column(nullable = false)
    private boolean roadsideOptIn;

    /** Demo-only drivers that auto-bid when dev tools are enabled. */
    @Column(nullable = false)
    private boolean demoBot;

    private Double lastLat;
    private Double lastLng;
    private Double heading;
    private Instant lastLocationAt;
    private String rejectionReason;
}
