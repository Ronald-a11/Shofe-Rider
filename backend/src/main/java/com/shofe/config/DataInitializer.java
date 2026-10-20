package com.shofe.config;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.Role;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Order(20)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        user("Shofe Admin", "admin@shofe.app", "+27110000001", "Admin123!", Role.ADMIN, 0, 0, null);
        user("Thabo Maseko", "rider@shofe.app", "+27110000002", "Rider123!", Role.RIDER, 4.9, 36, null);
        driver("Sipho Dlamini", "driver@shofe.app", "+27110000003", false, -26.2055, 28.0450, "Toyota", "Corolla", "White", "CA 123-456", 4.9, 842);
        driver("Amara Nkosi", "amara@shofe.app", "+27110000004", true, -26.1988, 28.0502, "Hyundai", "i20", "Blue", "CA 990-114", 4.8, 1205);
        driver("Johan Pietersen", "johan@shofe.app", "+27110000005", true, -26.2114, 28.0566, "Volkswagen", "Polo", "Silver", "CA 778-221", 4.7, 410);
    }

    private void driver(String name, String email, String phone, boolean bot, double lat, double lng,
                        String make, String model, String color, String plate, double rating, int trips) {
        User user = user(name, email, phone, "Driver123!", Role.DRIVER, rating, trips, null);
        DriverProfile profile = new DriverProfile();
        profile.setUser(user);
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setLicenseNumber("GP" + phone.substring(phone.length() - 4));
        profile.setVehicleMake(make);
        profile.setVehicleModel(model);
        profile.setVehicleColor(color);
        profile.setPlateNumber(plate);
        profile.setVehicleYear(2022);
        profile.setTripCount(trips);
        profile.setOnline(true);
        profile.setRoadsideOptIn(true);
        profile.setDemoBot(bot);
        profile.setLastLat(lat);
        profile.setLastLng(lng);
        profile.setLastLocationAt(Instant.now());
        user.setDriverProfile(profile);
        users.save(user);
    }

    private User user(String name, String email, String phone, String password, Role role, double rating, int ratings, DriverProfile ignored) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setRatingAvg(rating);
        user.setRatingCount(ratings);
        return users.save(user);
    }
}
