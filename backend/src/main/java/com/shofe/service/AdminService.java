package com.shofe.service;

import com.shofe.domain.entity.Ride;
import com.shofe.domain.entity.SosEvent;
import com.shofe.domain.enums.RequestStatus;
import com.shofe.domain.enums.RideStatus;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Value("${shofe.commission.percent:12}")
    private BigDecimal commissionPercent;

    @Value("${shofe.night-owl.zone:Africa/Harare}")
    private String zoneId;

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
    public MiscDtos.AdminAnalytics analytics() {
        ZoneId zone = ZoneId.of(zoneId);
        LocalDate today = LocalDate.now(zone);
        Map<LocalDate, MiscDtos.DayCount> days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            days.put(day, new MiscDtos.DayCount(day.format(DateTimeFormatter.ISO_LOCAL_DATE), 0, BigDecimal.ZERO));
        }

        BigDecimal revenue = BigDecimal.ZERO;
        long completed = 0;
        long cancelled = 0;
        long nightOwl = 0;
        for (Ride ride : rides.findAll()) {
            if (ride.getStatus() == RideStatus.COMPLETED) {
                completed++;
                revenue = revenue.add(ride.getFare());
                LocalDate day = ride.getCompletedAt() != null
                        ? ride.getCompletedAt().atZone(zone).toLocalDate()
                        : ride.getCreatedAt().atZone(zone).toLocalDate();
                MiscDtos.DayCount existing = days.get(day);
                if (existing != null) {
                    days.put(day, new MiscDtos.DayCount(
                            existing.day(),
                            existing.rides() + 1,
                            existing.revenue().add(ride.getFare())));
                }
            } else if (ride.getStatus() == RideStatus.CANCELLED) {
                cancelled++;
            }
            if (ride.isNightOwl()) {
                nightOwl++;
            }
        }
        BigDecimal commission = revenue.multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new MiscDtos.AdminAnalytics(
                revenue.setScale(2, RoundingMode.HALF_UP),
                commission,
                completed,
                cancelled,
                nightOwl,
                sosEvents.count(),
                new ArrayList<>(days.values()));
    }

    @Transactional(readOnly = true)
    public List<MiscDtos.SosEventView> sosFeed() {
        return sosEvents.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toSos)
                .toList();
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

    private MiscDtos.SosEventView toSos(SosEvent event) {
        return new MiscDtos.SosEventView(
                event.getId(),
                event.getUser().getFullName(),
                event.getUser().getPhone(),
                event.getRide() == null ? null : event.getRide().getId(),
                event.getLat(),
                event.getLng(),
                event.getNote(),
                event.getCreatedAt());
    }
}
