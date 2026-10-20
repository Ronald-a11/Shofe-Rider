package com.shofe.service;

import com.shofe.domain.enums.RequestStatus;
import com.shofe.domain.enums.RideStatus;
import com.shofe.domain.enums.Role;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.AuthDtos;
import com.shofe.dto.MiscDtos;
import com.shofe.dto.RideDtos;
import com.shofe.repo.DriverProfileRepository;
import com.shofe.repo.RideRepository;
import com.shofe.repo.RideRequestRepository;
import com.shofe.repo.SosEventRepository;
import com.shofe.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final RideRequestRepository requests;
    private final RideRepository rides;
    private final SosEventRepository sosEvents;
    private final UserMapper userMapper;
    private final RideMapper rideMapper;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public MiscDtos.AdminStats stats() {
        return new MiscDtos.AdminStats(
                users.count(),
                drivers.findByVerificationStatusOrderByUser_FullNameAsc(VerificationStatus.PENDING).size(),
                requests.findByStatusInOrderByCreatedAtDesc(EnumSet.of(RequestStatus.REQUESTED, RequestStatus.OFFERS_RECEIVED)).size(),
                rides.countByStatus(RideStatus.ACCEPTED) + rides.countByStatus(RideStatus.DRIVER_ARRIVED) + rides.countByStatus(RideStatus.IN_PROGRESS),
                rides.countByStatus(RideStatus.COMPLETED),
                sosEvents.count());
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UserResponse> users() {
        return users.findAll().stream().map(userMapper::toUser).toList();
    }

    @Transactional(readOnly = true)
    public List<RideDtos.RideResponse> rides() {
        var admin = currentUserService.require();
        return rides.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(ride -> rideMapper.toRide(ride, admin))
                .toList();
    }
}
