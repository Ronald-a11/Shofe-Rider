package com.shofe.service;

import com.shofe.domain.entity.NightCheckIn;
import com.shofe.domain.entity.User;
import com.shofe.dto.MiscDtos;
import com.shofe.repo.EmergencyContactRepository;
import com.shofe.repo.NightCheckInRepository;
import com.shofe.repo.RideRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Night Owl module (PDF Section 3): auto night window, check-ins, quick-help, emergency numbers.
 */
@Slf4j
@Service
public class NightOwlService {

    private final ZoneId zone;
    private final int startHour;
    private final int endHour;
    private final CurrentUserService currentUserService;
    private final NightCheckInRepository checkIns;
    private final RideRepository rides;
    private final EmergencyContactRepository contacts;
    private final NotificationService notifications;

    public NightOwlService(
            @Value("${shofe.night-owl.zone}") String zone,
            @Value("${shofe.night-owl.start-hour}") int startHour,
            @Value("${shofe.night-owl.end-hour}") int endHour,
            CurrentUserService currentUserService,
            NightCheckInRepository checkIns,
            RideRepository rides,
            EmergencyContactRepository contacts,
            NotificationService notifications) {
        this.zone = ZoneId.of(zone);
        this.startHour = startHour;
        this.endHour = endHour;
        this.currentUserService = currentUserService;
        this.checkIns = checkIns;
        this.rides = rides;
        this.contacts = contacts;
        this.notifications = notifications;
    }

    public boolean inWindow(Instant instant) {
        int hour = ZonedDateTime.ofInstant(instant, zone).getHour();
        if (startHour < endHour) {
            return hour >= startHour && hour < endHour;
        }
        return hour >= startHour || hour < endHour;
    }

    public boolean active(boolean requested) {
        return requested || inWindow(Instant.now());
    }

    /**
     * Approximate local night using solar elevation proxy: night when sun altitude &lt; -0.83°.
     * Falls back to configured hours if latitude is missing.
     */
    public boolean autoNightAt(Double lat, Double lng, Instant instant) {
        if (lat == null || lng == null) {
            return inWindow(instant);
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zone);
        double altitude = solarAltitudeDegrees(lat, zdt);
        return altitude < -0.83;
    }

    public MiscDtos.NightOwlStatus status(Double lat, Double lng, boolean manualOn) {
        Instant now = Instant.now();
        boolean window = inWindow(now);
        boolean solar = autoNightAt(lat, lng, now);
        boolean active = manualOn || window || solar;
        return new MiscDtos.NightOwlStatus(
                active,
                manualOn,
                window,
                solar,
                zone.getId(),
                startHour,
                endHour,
                emergencyForCountry(countryFromCoord(lat, lng)),
                "High-contrast Night Owl UI recommended while active");
    }

    @Transactional
    public MiscDtos.CheckInResponse checkIn(MiscDtos.CheckInRequest request) {
        User user = currentUserService.require();
        NightCheckIn row = new NightCheckIn();
        row.setUser(user);
        row.setOk(request.ok());
        row.setLat(request.lat());
        row.setLng(request.lng());
        row.setNote(request.note());
        if (request.rideId() != null) {
            rides.findById(request.rideId()).ifPresent(row::setRide);
        }
        checkIns.save(row);
        if (!request.ok()) {
            contacts.findByUserIdOrderByNameAsc(user.getId()).forEach(contact ->
                    log.warn("Night Owl escalation -> {} {} from {} (check-in failed)",
                            contact.getName(), contact.getPhone(), user.getFullName()));
            notifications.push(user, "Night Owl check-in missed",
                    "Guardians and support were notified because you did not confirm you are OK.");
        }
        return new MiscDtos.CheckInResponse(row.getId(), row.isOk(), row.getCreatedAt(),
                request.ok() ? "Glad you are OK" : "Escalation logged");
    }

    public List<MiscDtos.HelpPlace> quickHelp(double lat, double lng) {
        // Demo catalogue relative to the request point (swap for Google Places / Overpass later).
        record Seed(String type, String name, double dLat, double dLng, boolean open24h) {}
        List<Seed> seeds = List.of(
                new Seed("FUEL", "Night Fuel Stop", 0.008, 0.004, true),
                new Seed("HOSPITAL", "24h Clinic", -0.006, 0.009, true),
                new Seed("POLICE", "Police Station", 0.011, -0.005, true),
                new Seed("MECHANIC", "Roadside Mechanic", -0.01, -0.008, false),
                new Seed("SAFE_SPOT", "Well-lit Mall Parking", 0.004, -0.012, true));
        List<MiscDtos.HelpPlace> places = new ArrayList<>();
        for (Seed seed : seeds) {
            double pLat = lat + seed.dLat;
            double pLng = lng + seed.dLng;
            double km = GeoMath.haversineKm(lat, lng, pLat, pLng);
            places.add(new MiscDtos.HelpPlace(seed.type, seed.name, pLat, pLng, km, seed.open24h));
        }
        places.sort(Comparator.comparingDouble(MiscDtos.HelpPlace::distanceKm));
        return places;
    }

    public MiscDtos.EmergencyInfo emergencyForCountry(String countryCode) {
        Map<String, MiscDtos.EmergencyInfo> map = Map.of(
                "ZW", new MiscDtos.EmergencyInfo("ZW", "999", "994", "993", "Zimbabwe police / ambulance / fire"),
                "ZA", new MiscDtos.EmergencyInfo("ZA", "10111", "10177", "10111", "South Africa"),
                "NG", new MiscDtos.EmergencyInfo("NG", "112", "112", "112", "Nigeria"),
                "KE", new MiscDtos.EmergencyInfo("KE", "999", "999", "999", "Kenya"),
                "GB", new MiscDtos.EmergencyInfo("GB", "999", "999", "999", "United Kingdom"),
                "US", new MiscDtos.EmergencyInfo("US", "911", "911", "911", "United States"));
        return map.getOrDefault(countryCode == null ? "ZW" : countryCode.toUpperCase(),
                new MiscDtos.EmergencyInfo("ZZ", "112", "112", "112", "International default"));
    }

    private String countryFromCoord(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "ZW";
        }
        // Coarse country bounds — replace with a reverse-geocoder later.
        if (lat > -23 && lat < -15 && lng > 24 && lng < 34) return "ZW";
        if (lat > -35 && lat < -22 && lng > 16 && lng < 33) return "ZA";
        if (lat > 4 && lat < 14 && lng > 2 && lng < 15) return "NG";
        if (lat > -5 && lat < 5 && lng > 33 && lng < 42) return "KE";
        return "ZW";
    }

    private double solarAltitudeDegrees(double lat, ZonedDateTime zdt) {
        int day = zdt.getDayOfYear();
        LocalTime time = zdt.toLocalTime();
        double hour = time.getHour() + time.getMinute() / 60.0;
        double decl = 23.44 * Math.sin(Math.toRadians((360.0 / 365.0) * (day - 81)));
        double ha = 15.0 * (hour - 12.0);
        double sinAlt = Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(decl))
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(decl)) * Math.cos(Math.toRadians(ha));
        return Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sinAlt))));
    }
}
